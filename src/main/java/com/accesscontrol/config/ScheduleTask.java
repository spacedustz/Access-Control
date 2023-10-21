package com.accesscontrol.config;

import com.accesscontrol.entity.Event;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import com.accesscontrol.service.RabbitTopicListener;
import com.accesscontrol.service.RecycleFn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author: 신건우
 * @desc
 * run() 함수 : Spring 어플리케이션 시작 시, DB에 데이터가 하나도 없으면 초기 데이터 컬럼 생성
 * addData() 함수 : DB에 객체가 1개 이상이고, 데이터의 날짜가 오늘 날짜가 아닐때 오늘 날짜에 해당하는 객체 새로 생성
 * healthCheck() 함수 : 10초 마다 운영시간인지 확인 후, 운영시간이 아니면 객체의 Status 상태를 변화 후 소켓에 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTask {
    private final EventRepository eventRepository;
    private final RecycleFn recycleFn;
    private final SimpMessagingTemplate template;
    private final RabbitTopicListener rabbitTopicListener;

    @Scheduled(cron = "1 0 0 * * *", zone = "Asia/Seoul")
    public void scheduleTask() throws Exception {
        rabbitTopicListener.createEntity();
        log.info("데이터 생성 태스크 실행 - 시간 : {}", LocalDateTime.now());
    }

    // 10초 마다 운영시간인지 체크해서 현황판의 Status를 변화 시키는 Scheduler
    @Scheduled(cron = "0/5 * * * * *")
    public void healthCheck() {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            recycleFn.validateOperationTime(event);
            eventRepository.save(event);
            template.convertAndSend("/count/data", event);
        } catch (Exception e) {
            log.error("객체 조회 실패", e);
        }
    }
}
