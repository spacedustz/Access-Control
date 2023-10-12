package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import com.accesscontrol.entity.Status;
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

    // Event 객체의 Status 값 업데이트
    public Event updateCustomStatus(String status) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
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
            event = recycleFn.getEntity(recycleFn.getEntityCount());
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
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            event.setInCount(event.getInCount() + num);
            event.setOccupancy(event.getInCount() - event.getOutCount());

            recycleFn.validateOccupancy(event);
            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
            template.convertAndSend("/count/occupancy", event);
            log.info("재실 인원 값 [증가] 성공 - 증가한 수치 : {}, 반영된 현재 방안 인원 수치 : {}", num, event.getOccupancy());
        } catch (Exception e) {
            log.error("재실 인원 수 조정 실패 [증가]", e);
        }
    }

    // 재실 인원 값 감소 함수
    public void decreaseOccupancy(int num) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());

            event.setInCount(event.getInCount() - num);
            event.setOccupancy(event.getInCount() - event.getOutCount());

            recycleFn.validateOccupancy(event);
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
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            event.setOpenTime(time);
            eventRepository.save(event);
            template.convertAndSend("/count/time", event);
            log.info("운영 시작 시간 변경 완료");
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // 운영 종료 시간 변경
    public void setCloseTime(String time) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            event.setCloseTime(time);
            eventRepository.save(event);
            template.convertAndSend("/count/time", event);
            log.info("운영 종료 시간 변경 완료");
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // 운영 시간 조회
    @Transactional(readOnly = true)
    public void getOperationTime() {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            template.convertAndSend("/count/time", event);
        } catch (Exception e) {
            log.error("Event 영업시간 로드 실패", e);
        }
    }

    // RelayURL 변경
    public void setRelayUrl(String url) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            event.setRelayUrl(url);
            eventRepository.save(event);
            template.convertAndSend("/count/relay", event);
        } catch (Exception e) {
            log.error("Event Relay URL 변경 실패 - Event ID : {}", event.getId());
        }
    }

    // 데이터 로드용
    public Event getInitData() {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
            recycleFn.validateOperationTime(event);
            Status status = event.getStatus();

            recycleFn.autoUpdateStatus(event);
            eventRepository.save(event);
            log.info("Event 객체 데이터 로드 완료 - Event ID : {}", event.getId());
            template.convertAndSend("/count/data", event);
        } catch (Exception e) {
            log.error("Event 객체 데이터 로드 실패", e);
        }

        return event;
    }
}
