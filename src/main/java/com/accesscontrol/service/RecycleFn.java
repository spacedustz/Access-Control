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

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Cacheable("entity")
    public Event getEntity(Long pk) {
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));
    }

    public void autoUpdateStatus(Event event) {
        if (event.getOccupancy() <= 9) {
            event.setStatus(Status.LOW);
        } else if (event.getOccupancy() >= 10 && event.getOccupancy() < event.getMaxCount()) {
            event.setStatus(Status.MEDIUM);
        } else if (event.getOccupancy() >= event.getMaxCount()) {
            event.setStatus(Status.HIGH);
        }
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

    public void validateOccupancy(Event event) {
        if (event.getOccupancy() < 0) {
            initiateCount(event);
            log.error("재실 인원은 0명 이하가 될 수 없습니다. - 초기화 된 방안 인원 수치 : {}", event.getOccupancy());
        }
    }

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
            log.info("정상 운영 시간 입니다. - 현재 시간 : {}", now);
            autoUpdateStatus(event);
        } else {
            event.setStatus(Status.NOT_OPERATING);
            initiateCount(event);
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 현재 시간 : {}", openTime, closeTime, now);
        }

        return event;
    }
}
