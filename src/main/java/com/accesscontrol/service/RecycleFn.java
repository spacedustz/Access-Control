package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import com.accesscontrol.entity.Status;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RecycleFn {
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate template;
    private String currentDate = String.valueOf(LocalDate.now());

    @Transactional(readOnly = true)
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Transactional(readOnly = true)
    public Event getEntity(Long pk) {
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));
    }

    // 엔티티 수치 초기화 함수
    public void initiateCount(Event event) {
        event.setOccupancy(0);
        event.setInCount(0);
        event.setOutCount(0);
    }

    // 년-월-일 변환 함수
    public String ymdFormatter(@Nullable LocalDateTime dateTime) {
        DateTimeFormatter YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTime.format(YMDFormatter);
    }

    // 시-분 변환 함수
    public String hmFormatter(@Nullable LocalDateTime date) {
        DateTimeFormatter HMFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return date.format(HMFormatter);
    }

    // 현재 인원에 따른 객체 상태 업데이트
    public void autoUpdateStatus(Event event) {
        if (event.getCustomStatus().equals("")) {
            if (event.getOccupancy() <= 9) {
                event.setStatus(Status.LOW);
            } else if (event.getOccupancy() >= 10 && event.getOccupancy() < event.getMaxCount()) {
                event.setStatus(Status.MEDIUM);
            } else if (event.getOccupancy() >= event.getMaxCount()) {
                event.setStatus(Status.HIGH);
            }
        } else {
            log.info("현재 방안의 상태 - {}", event.getCustomStatus());
        }
    }

    // 운영 시간 검증 함수
    public Event validateOperationTime(Event event) {
        String openTime = event.getOpenTime();
        String closeTime = event.getCloseTime();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalDateTime nowTime = LocalDateTime.now();

        LocalTime now = LocalTime.parse(hmFormatter(nowTime));

        // openTime과 closeTime을 LocalDateTime으로 변환
        LocalTime open = LocalTime.parse(openTime, timeFormatter);
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);

        // 운영 시간 검증
        if (now.isAfter(open) && now.isBefore(close)) {
            log.info("정상 운영 시간 입니다. [ 운영 시간 ] {} - {}", open, close);
            autoUpdateStatus(event);
        } else {
            event.setStatus(Status.NOT_OPERATING);
            initiateCount(event);
            log.info("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 현재 시간 : {}", openTime, closeTime, now);
        }

        return event;
    }

    // 재실 인원 검증 함수
    public void validateOccupancy(Event event) {
        int occupancy = event.getInCount() - event.getOutCount();
        int max = event.getMaxCount();

        try {
            if (occupancy < 0) {
                initiateCount(event);
                eventRepository.save(event);

                log.warn("재실 인원 오류 - In/Out Count, Occupancy 초기화 - 초기화 된 Occupancy 값 : {}", event.getOccupancy());
            }

            if (occupancy > max) {
                event.setInCount(15);
                event.setOutCount(0);
                event.setOccupancy(event.getMaxCount());
                eventRepository.save(event);
                log.warn("인원 초과 - 재실 인원을 최대 인원 수로 변경 : {}", event.getOccupancy());
            }

            template.convertAndSend("/count/data", event);
        } catch (Exception e) {
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId(), e);
            throw new CommonException("Validate-Occupancy", HttpStatus.INTERNAL_SERVER_ERROR);
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
                                        Event event) {

        // 이벤트 데이터의 날짜 검증
        if (!eventYMDDate.equals(currentDate) || (!entityYMDDate.equals(currentDate))) {
            log.warn("데이터의 날짜가 오늘 날짜가 아닙니다. - 현재 날짜 : {}, 데이터의 날짜 : {}", currentDate, eventYMDDate);
        }

        // 이벤트 데이터의 운영 시간 검증
        if (!eventDateTime.isAfter(open) && !eventDateTime.isBefore(close)) {
            event.setStatus(Status.NOT_OPERATING);
            log.info("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, eventDateTime);
        }

        try {
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());
            throw new CommonException("Validate-Operation-Time", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Web Socket Session에 Event 객체 전달
        template.convertAndSend("/count/data", event);
    }
}
