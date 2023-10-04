package com.accesscontrol.service;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.entity.Event;
import com.accesscontrol.entity.Status;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {
    private String currentDate = String.valueOf(LocalDate.now());
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate template;
    private final RecycleFn recycleFn;
    private final RestTemplate restTemplate;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Cacheable("entity")
    public Event getEntity(Long pk) {
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));
    }

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "q.frame")
    public void receive(EventDTO message) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
        } catch (Exception e) {
            log.error("DATA-001 : 엔티티 조회 실패");
            throw new CommonException("DATA-001 : 엔티티 조회 실패", HttpStatus.NOT_FOUND);
        }

        String openTime = event.getOpenTime();
        String closeTime = event.getCloseTime();

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM  d HH:mm:ss yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);

        // DB에 저장된 데이터의 날짜 나누기
        String entityYMDDate = recycleFn.ymdFormatter(event.getEventTime()); // YYYY-MM-DD
        String entityHMDate = recycleFn.hmFormatter(event.getEventTime()); // HH-MM

        // 이벤트로 넘어온 데이터의 날짜 나누기
        String eventYMDDate = recycleFn.ymdFormatter(convertedDate); // YYYY-MM-DD
        String eventHMDate = recycleFn.hmFormatter(convertedDate); // HH-MM

        // 이벤트로 넘어온 데이터의 시간이 운영시간 범위에 존재하는지 확인하기 위한 LocalTime 타입 변환
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime open = LocalTime.parse(openTime, timeFormatter);
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);
        LocalTime eventDateTime = LocalTime.parse(eventHMDate, timeFormatter);

        // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증
        validateOperatingStatus(entityYMDDate, eventYMDDate, open, close, eventDateTime, openTime, closeTime, entityHMDate, event);

        // 이벤트로 넘어온 데이터의 Direction 가져오기
        List<String> directions = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();

        for (String direction : directions) {
            // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증
            validateOperatingStatus(entityYMDDate, eventYMDDate, eventDateTime, open, close, openTime, closeTime, entityHMDate, event);

            // 현재 재실 인원이 마이너스(-)로 가는 비정상적인 상황 발생 시 in/out count, occupancy 값 초기화
            if (direction.equalsIgnoreCase("down")) {
                event.setInCount(event.getInCount() + 1);
                log.info("입장");
            } else if (direction.equalsIgnoreCase("up")) {
                event.setOutCount(event.getOutCount() + 1);
                log.info("퇴장");
            }

            if (event.getInCount() - event.getOutCount() < 0) {
                validateOccupancy(event);
            }

            event.setOccupancy(event.getInCount() - event.getOutCount());
            log.info("재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());

            recycleFn.autoUpdateStatus(event);

            eventRepository.save(event);

            // Web Socket Session 에 Event 객체 전달
            template.convertAndSend("/count/data", event);
            requestApi(event);
        }
    }

    // Door API에 HTTP Request 요청
    public void requestApi(Event event) {
        // URL 설정
        String url = event.getRelayUrl();

        // 요청 보내기
        restTemplate.postForLocation(url, null);
    }

    // 재실 인원 검증 함수
    public void validateOccupancy(Event event) {
        try {
            if (event.getOccupancy() < 0) {
                recycleFn.initiateCount(event);
                eventRepository.save(event);
                template.convertAndSend("/count/data", event);

                log.error("재실 인원 오류 - In/Out Count, Occupancy 초기화 - 초기화 된 Occupancy 값 : {}", event.getOccupancy());
            }

            if (event.getOccupancy() >= event.getMaxCount()) {
                log.info("인원 초과 - 재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());
            }
        } catch (Exception e) {
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId(), e);
        }
    }

    // 운영시간 검증 함수
    public void validateOperatingStatus(String entityYMDDate,
                                        String eventYMDDate,
                                        LocalTime open,
                                        LocalTime close,
                                        LocalTime eventDateTime,
                                        String openTime,
                                        String closeTime,
                                        String entityHMDate,
                                        Event event) {

        if (!entityYMDDate.equals(currentDate) || !eventYMDDate.equals(currentDate) || (!eventDateTime.isAfter(open) && eventDateTime.isBefore(close))) {
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, entityHMDate);
            recycleFn.initiateCount(event);
            event.setStatus(Status.NOT_OPERATING);

            try {
                eventRepository.save(event);
            } catch (Exception e) {
                log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());
            }
        }
    }
}
