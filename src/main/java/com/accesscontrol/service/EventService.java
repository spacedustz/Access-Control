package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Cacheable("entity")
    public Event getEntity(Long pk) {
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));
    }

    // Event 객체의 Status 값 업데이트
    public String updateStatus(String status) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setStatus(status);
            eventRepository.save(event);
        } catch (Exception e) {
            assert event != null;
            log.error("Event 객체 Statue Update 실패 - Event ID : {}, Status 변경 여부 : {}", event.getId(), event.getStatus());
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);
        }

        log.info("Event 객체 상태 업데이트 완료 - 상태 : {}", event.getStatus());
        return event.getStatus();
    }

    // maxCount 값 업데이트
    public Event updateMaxCount(String max) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setMaxCount(Integer.parseInt(max));
            eventRepository.save(event);
        } catch (Exception e) {
            assert event != null;
            log.error("Event 객체 maxCount Update 실패 - Event ID : {}, maxCount 변경 여부 : {}", event.getId(), event.getMaxCount());
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);
        }

        log.info("Event 객체 최대 인원 업데이트 완료 - 최대 인원 : {}", event.getMaxCount());
        return event;
    }

    // 초기 데이터 로드용
    @Transactional(readOnly = true)
    public Event getInitData() {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
        } catch (Exception e) {
            assert false;
            log.error("초시 Event 객체 로드 실패 - Event ID : {}", event.getId());
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);
        }

        log.info("Event 객체 초기 로드 완료 - Event ID : {}", event.getId());
        return event;
    }
}