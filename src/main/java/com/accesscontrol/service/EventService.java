package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate template;
    private final RecycleFn recycleFn;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    @Cacheable("entity")
    public Event getEntity(Long pk) {
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));
    }

    // Event 객체의 Status 값 업데이트
    public Event updateCustomStatus(String status) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setCustomStatus(status);
            eventRepository.save(event);
            template.convertAndSend("/count/customStatus", event);
        } catch (Exception e) {
            assert event != null;
            log.error("Event 객체 Statue Update 실패 - Event ID : {}, Status 변경 여부 : {}", event.getId(), event.getStatus());
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);
        }

        log.info("Event 객체 상태 업데이트 완료 - 상태 : {}", event.getCustomStatus());
        return event;
    }

    // maxCount 값 업데이트
    public Event updateMaxCount(String max) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setMaxCount(Integer.parseInt(max));
            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
            template.convertAndSend("/count/data", event);
        } catch (Exception e) {
            assert event != null;
            log.error("Event 객체 maxCount Update 실패 - Event ID : {}, maxCount 변경 여부 : {}", event.getId(), event.getMaxCount());
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);
        }

        log.info("Event 객체 최대 인원 업데이트 완료 - 최대 인원 : {}", event.getMaxCount());
        return event;
    }

    // 재실 인원 값 증가 함수
    public void increaseOccupancy(int num) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setInCount(event.getOutCount() + num);
            event.setOccupancy(event.getOccupancy() + num);

            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
            template.convertAndSend("/count/occupancy", event);
            log.info("재실 인원 값 [증가] 성공 - 감소한 수치 : {}, 반영된 현재 방안 인원 수치 : {}", num, event.getOccupancy());
        } catch (Exception e) {
            log.error("재실 인원 수 조정 실패 [증가]", e);
        }
    }

    // 재실 인원 값 감소 함수
    public void decreaseOccupancy(int num) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setOutCount(event.getInCount() - num);
            event.setOccupancy(event.getOccupancy() - num);

            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
            template.convertAndSend("/count/occupancy", event);
            log.info("재실 인원 값 [감소] 성공 - 감소한 수치 : {}, 반영된 현재 방안 인원 수치 : {}", num, event.getOccupancy());
        } catch (Exception e) {
            log.error("재실 인원 수 조정 실패 [감소]", e);
        }
    }

    // 운영 시작 시간 변경
    public void setOpenTime(String time) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setOpenTime(time);
            eventRepository.save(event);
            template.convertAndSend("/count/time", event);
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // 운영 종료 시간 변경
    public void setCloseTime(String time) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setCloseTime(time);
            eventRepository.save(event);
            template.convertAndSend("/count/time", event);
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // 운영 시간 조회
    @Transactional(readOnly = true)
    public void getOperationTime() {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            template.convertAndSend("/count/time", event);
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // RelayURL 변경
    public void setRelayUrl(String url) {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            event.setRelayUrl(url);
            eventRepository.save(event);
            template.convertAndSend("/count/relay", event);
        } catch (Exception e) {
            log.error("Event Relay URL 변경 실패 - Event ID : {}", event.getId());
        }
    }

    // 데이터 로드용
    @Transactional(readOnly = true)
    public Event getInitData() {
        Event event = null;

        try {
            event = getEntity(getEntityCount());
            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Event 객체 데이터 로드 실패", e);
        }

        assert event != null;
        log.info("Event 객체 데이터 로드 완료 - Event ID : {}", event.getId());
        return event;
    }
}
