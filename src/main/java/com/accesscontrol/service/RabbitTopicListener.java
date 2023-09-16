package com.accesscontrol.service;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.entity.Event;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {

    private EventRepository eventRepository;

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "test.queue")
    public void receive(EventDTO message) {
        log.info("원본 Date: " + message.getSystem_date());
        log.info("원본 Count: " + message.getEvents().stream().map(it -> it.getExtra().getCount()).toList());

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:s yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);
        log.info("날짜 타입 변환 테스트 : " + convertedDate);

        // DTO -> Entity -> Repository
        Event event = Event.createOf(convertedDate);
        event.setCount(event.getCount() + 1);

//        System.out.println("Received Message: " + message);
//        Count entity = Count.createOf(message.getExtra().getCount(), message.getEventTime());
//        countRepository.save(entity);
//        log.info("Entity", entity);
    }
}
