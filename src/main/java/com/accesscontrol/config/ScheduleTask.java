package com.accesscontrol.config;

import com.accesscontrol.entity.Event;
import com.accesscontrol.repository.EventRepository;
import com.accesscontrol.service.RabbitTopicListener;
import com.accesscontrol.service.RecycleFn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    // 3초 마다 운영시간인지 체크해서 현황판의 Status를 변화 시키는 Scheduler
    @Scheduled(cron = "0/3 * * * * *")
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
