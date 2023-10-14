package com.accesscontrol.service;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.entity.Event;
import com.accesscontrol.repository.EventRepository;
import com.accesscontrol.thread.InstanceMonitoringThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate template;
    private final RecycleFn recycleFn;
    private final RestTemplate restTemplate;

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "q.frame")
    public void receive(EventDTO message) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
        } catch (Exception e) {
            log.error("DATA-001 : 엔티티 조회 실패");
        }

        String openTime = event.getOpenTime();
        String closeTime = event.getCloseTime();

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM  d HH:mm:ss yyyy", Locale.ENGLISH);
//        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);

        // DB에 저장된 데이터의 날짜 나누기
        String entityYMDDate = recycleFn.ymdFormatter(event.getEventTime()); // 객체의 YYYY-MM-DD 날짜
        String entityHMDate = recycleFn.hmFormatter(event.getEventTime()); // 객체의 HH-MM 날짜

        // 이벤트로 넘어온 데이터의 날짜 나누기
        String eventYMDDate = recycleFn.ymdFormatter(convertedDate); // 이벤트 데이터의 YYYY-MM-DD 날짜
        String eventHMDate = recycleFn.hmFormatter(convertedDate); // 이벤트 데이터의 HH-MM 날짜

        // 이벤트로 넘어온 데이터의 시간이 운영시간 범위에 존재하는지 확인하기 위한 LocalTime 타입 변환
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime open = LocalTime.parse(openTime, timeFormatter); // event.getOpenTime()
        LocalTime close = LocalTime.parse(closeTime, timeFormatter); // event.getCloseTime()
        LocalTime eventDateTime = LocalTime.parse(eventHMDate, timeFormatter);

        // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증
        recycleFn.validateOperatingStatus(entityYMDDate, eventYMDDate, open, close, eventDateTime, openTime, closeTime, event);

        // 이벤트로 넘어온 데이터의 Direction 가져오기
        List<String> directions = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();

        for (String direction : directions) {
            // 현재 재실 인원이 마이너스(-)로 가는 비정상적인 상황 발생 시 in/out count, occupancy 값 초기화
            if (direction.equalsIgnoreCase("down")) {
                event.setInCount(event.getInCount() + 1);
                log.info("입장");
                requestApi(event); // Request Door API
            } else if (direction.equalsIgnoreCase("up")) {
                event.setOutCount(event.getOutCount() + 1);
                log.info("퇴장");
            }

            if (event.getInCount() - event.getOutCount() < 0) {
                recycleFn.validateOccupancy(event);
            }

            event.setOccupancy(event.getInCount() - event.getOutCount());
            log.info("재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());

            recycleFn.autoUpdateStatus(event);

            eventRepository.save(event);

            // Web Socket Session 에 Event 객체 전달
            template.convertAndSend("/count/data", event);
        }
    }

    // Door API에 HTTP Request 요청
    public void requestApi(Event event) {
        // URL 설정
        String url = event.getRelayUrl();

        // 요청 보내기
        restTemplate.getForEntity(url, Void.class);
    }
}
