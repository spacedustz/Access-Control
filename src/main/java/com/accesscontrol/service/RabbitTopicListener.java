package com.accesscontrol.service;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.entity.Event;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {

    private EventRepository eventRepository;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "q.frame")
    public void receive(EventDTO message) {
        log.info("원본 Date: {}", message.getSystem_date());
        log.info("원본 Count: " + message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList());

        String direction = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList().get(0);
        log.info("Direction: {}", direction);

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:s yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);

        // 년월일, 시분초 변환기
        DateTimeFormatter YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter HMFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Event event = eventRepository.findById(getEntityCount()).orElse(null);

        // DB에 저장된 데이터의 날짜 나누기
        String entityYMDDate = event.getEventTime().format(YMDFormatter);
        String entityHMDate = event.getEventTime().format(HMFormatter);

        // 이벤트로 넘어온 데이터의 날짜 나누기
        String eventYMDDate = convertedDate.format(YMDFormatter);
        String eventHMDate = convertedDate.format(HMFormatter);

        message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();

        // 현재 Entity와 이벤트로 넘어온 년월일, 운영 시간일때만 Door Open
        if (entityYMDDate.equals(eventYMDDate) && entityHMDate.equals(eventHMDate)) {
        }


    }
}
