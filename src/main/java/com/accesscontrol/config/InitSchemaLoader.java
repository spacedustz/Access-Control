package com.accesscontrol.config;

import com.accesscontrol.entity.Event;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author: 신건우
 * @desc
 * 첫번째 if 문 : DB에 데이터가 하나도 없으면 초기 데이터 컬럼 생성
 * 두번째 if 문 : DB에 객체가 1개 이상이고, 데이터의 날짜가 오늘 날짜가 아닐때 오늘 날짜에 해당하는 객체 새로 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitSchemaLoader implements ApplicationRunner {

    private final EventRepository eventRepository;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 테이블에 데이터 수 확인
        long objectCount = getEntityCount();

        // DB에 데이터가 하나도 없으면 초기 데이터 생성
        if (objectCount == 0) {
            log.info("Event Table 내부에 데이터가 없습니다. 객체를 생성합니다.");

            Event event = Event.createOf(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));

            try {
                eventRepository.save(event);
                log.info("객체 생성 완료, Event ID: {}", event.getId());
            } catch (Exception e) {
                log.error("객체 생성 실패", e);
                throw new CommonException("INIT-001", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // DB에 데이터가 1개 이상이고, 그 데이터의 현재 년월일이 현재 년월일과 맞지 않으면 새로운 객체 생성
        if (objectCount > 0) {
            log.info("Event Table 내부에 데이터가 있습니다. 객체의 날짜가 현재 날짜와 동일한 지 검증 중...");

            Event storedEvent = null;
            try {
                storedEvent = eventRepository.findById(getEntityCount()).orElse(null);
            } catch (Exception e) {
                log.error("기존에 존재하는 데이터 조회 실패 - Event ID : {}", storedEvent.getId(), e);
                throw new CommonException("INIT-002", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            if (!storedEvent.getEventTime().format(formatter).equals(currentDate.toString())) {
                log.info("이미 생성된 데이터 내부의 날짜와 현재 날짜 비교 중 ... Event ID: {}", storedEvent.getId());
                log.info("현재 날짜 : {} - 데이터 날짜 : {}", currentDate, storedEvent.getEventTime());
                Event event = Event.createOf(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));

                try {
                    eventRepository.save(event);
                    log.info("기존 데이터의 날짜와 현재 시간이 불일치합니다, 새로운 객체를 생성 합니다. - Event ID: {}", event.getId());
                } catch (Exception e) {
                    log.error("기존 데이터의 날짜와 현재 시간 불일치합니다, - 객체 생성 실패", e);
                    throw new CommonException("INIT-003", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                log.info("현재 날짜의 데이터가 이미 존재합니다, 객체 생성 중지 - 현재 데이터의 날짜 : {}, ID : {}", storedEvent.getEventTime().format(formatter), storedEvent.getId());
            }
        }
    }
}
