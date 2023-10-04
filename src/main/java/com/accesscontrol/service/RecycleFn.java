package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import com.accesscontrol.entity.Status;
import com.accesscontrol.repository.EventRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleFn {

    private final EventRepository eventRepository;

    public void autoUpdateStatus(Event event) {
        if (event != null) {
            if (event.getOccupancy() <= 9) {
                event.setStatus(Status.LOW);
            } else if (event.getOccupancy() >= 10 && event.getOccupancy() < event.getMaxCount()) {
                event.setStatus(Status.MEDIUM);
            } else if (event.getOccupancy() >= event.getMaxCount()) {
                event.setStatus(Status.HIGH);
            }
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
}
