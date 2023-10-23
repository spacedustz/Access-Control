package com.accesscontrol.service;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.entity.Event;
import com.accesscontrol.error.CommonException;
import com.accesscontrol.repository.EventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate template;
    private final RecycleFn recycleFn;
    private final RestTemplate restTemplate;

    @PostConstruct
    public void init() throws Exception {
        createEntity();
    }

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "count")
    public void receive(EventDTO message) {
        Event event = null;

        try {
            event = recycleFn.getEntity(recycleFn.getEntityCount());
        } catch (Exception e) {
            log.error("DATA-001 : 엔티티 조회 실패");
            throw new CommonException("Listener-Entity", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 운영 시간
        String openTime = event.getOpenTime();
        String closeTime = event.getCloseTime();

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = null;

        try {
            convertedDate = LocalDateTime.parse(openTime, formatter1);
        } catch (Exception e1) {
            try {
                convertedDate = LocalDateTime.parse(originalDate, formatter2);
            } catch (Exception e2) {
                log.error("날짜 형식이 두 케이스 모두 일치하지 않습니다.");
            }
        }

        // DB에 저장된 데이터의 날짜 나누기
        String entityYMDDate = recycleFn.ymdFormatter(event.getEventTime()); // 객체의 YYYY-MM-DD 날짜
        String entityHMDate = recycleFn.hmFormatter(event.getEventTime()); // 객체의 HH-MM 날짜

        // 이벤트로 넘어온 데이터의 날짜 나누기
        String eventYMDDate = recycleFn.ymdFormatter(convertedDate); // 이벤트 데이터의 YYYY-MM-DD 날짜
        String eventHMDate = recycleFn.hmFormatter(convertedDate); // 이벤트 데이터의 HH-MM 날짜

        // 이벤트로 넘어온 데이터의 시간이 운영시간 범위에 존재하는지 확인하기 위한 LocalTime 타입 변환
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime open = LocalTime.parse(openTime, timeFormatter); // event.getOpenTime()
        LocalTime close = LocalTime.parse(closeTime, timeFormatter); // event.getCloseTime()
        LocalTime eventDateTime = LocalTime.parse(eventHMDate, timeFormatter);

        // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증
        recycleFn.validateOperatingStatus(entityYMDDate, eventYMDDate, open, close, eventDateTime, openTime, closeTime, event);

        recycleFn.validateOccupancy(event);

        // 이벤트로 넘어온 데이터의 Direction 가져오기
        List<String> directions = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();

        for (String direction : directions) {
            // 현재 재실 인원이 마이너스(-)로 가는 비정상적인 상황 발생 시 in/out count, occupancy 값 초기화
            if (direction.equalsIgnoreCase("down")) {
                event.setInCount(event.getInCount() + 1);
                log.info("입장");
                requestApi(event); // Request Door API
                recycleFn.validateOccupancy(event);
                event.setOccupancy(event.getInCount() - event.getOutCount());
                recycleFn.autoUpdateStatus(event);
                eventRepository.save(event);
                template.convertAndSend("/count/data", event);

            } else if (direction.equalsIgnoreCase("up")) {
                event.setOutCount(event.getOutCount() + 1);
                log.info("퇴장");
                recycleFn.validateOccupancy(event);
                event.setOccupancy(event.getInCount() - event.getOutCount());
                recycleFn.autoUpdateStatus(event);
                eventRepository.save(event);
                template.convertAndSend("/count/data", event);
            }
        }
    }

    // Door API에 HTTP Request 요청
    public void requestApi(Event event) {
        // URL 설정
        String url = event.getRelayUrl();

        // 요청 보내기
        restTemplate.getForEntity(url, Void.class);
    }

    public void addData() throws Exception {

        // 테이블에 데이터 수 확인
        long objectCount = recycleFn.getEntityCount();

        // DB에 데이터가 1개 이상이고, 그 데이터의 현재 년월일이 현재 년월일과 맞지 않으면 새로운 객체 생성
        if (objectCount > 0) {
            log.info("Event Table 내부에 데이터가 있습니다. 객체의 날짜가 현재 날짜와 동일한 지 검증 중...");

            Event storedEvent = null;
            try {
                storedEvent = eventRepository.findById(recycleFn.getEntityCount()).orElse(null);
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
                    event.setRelayUrl("http://192.168.0.7/index.html?p0=1000");
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

    // Spring 서버 재시작 할때, DB에 데이터가 하나도 없을때 초기 데이터 1개 생성
    public void createEntity() throws Exception {
        // 테이블에 데이터 수 확인
        long objectCount = recycleFn.getEntityCount();

        // DB에 데이터가 하나도 없으면 초기 데이터 생성
        if (objectCount == 0) {
            log.info("Event Table 내부에 데이터가 없습니다. 객체를 생성합니다.");

            Event event = Event.createOf(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
            event.setRelayUrl("http://192.168.0.7/index.html?p0=1000");

            try {
                eventRepository.save(event);
                log.info("객체 생성 완료, Event ID: {}", event.getId());
            } catch (Exception e) {
                log.error("객체 생성 실패", e);
                throw new CommonException("INIT-001", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            addData();
        }
    }
}
