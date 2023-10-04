## 📘 **Event Service & Controller & RecycleFn**

브라우저에서 소켓에 접속해 백엔드인 Spring으로 부터 각종 데이터를 받아오기 위한 Rest API를 작성합니다.

두 클래스 다 값 조회, 변경의 간단한 API 이므로 설명은 생략하겠습니다.

<br>

**EventService**

```java
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
            event.setOutCount(event.getOutCount() + num);
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
            event.setInCount(event.getInCount() - num);
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
```

<br>

**EventController**

```java
@RestController  
@RequestMapping("/ws")  
@RequiredArgsConstructor  
public class EventController {  
    private final EventService eventService;  
  
    // Event 객체 Status 값 변경 API    
    @PatchMapping("/update-status")  
    public ResponseEntity<EventDTO.Response> updateStatus(@RequestParam String status) {  
        return new ResponseEntity<>(EventDTO.Response.fromEntity(eventService.updateCustomStatus(status)), HttpStatus.OK);  
    }  
  
    // Event 객체 MaxCount 값 변경 API    
    @PatchMapping("/update-max")  
    public ResponseEntity<EventDTO.Response> updateMaxCount(@RequestParam String max) {  
        return new ResponseEntity<>(EventDTO.Response.fromEntityForUpdateMaxCount(eventService.updateMaxCount(max)), HttpStatus.OK);  
    }  
  
    // 초기 데이터 로드용 API    
    @GetMapping("/init")  
    public ResponseEntity<EventDTO.Response> getInitData() {  
        return new ResponseEntity<>(EventDTO.Response.fromEntity(eventService.getInitData()), HttpStatus.OK);  
    }  
  
    // 현재 재실 인원 변경 API - 증가  
    @PatchMapping("/increase-occupancy")  
    public void increaseOccupancy(@RequestParam int num) {  
        eventService.increaseOccupancy(num);  
    }  
  
    // 현재 재실 인원 변경 API - 감소  
    @PatchMapping("/decrease-occupancy")  
    public void decreaseOccupancy(@RequestParam int num) {  
        eventService.decreaseOccupancy(num);  
    }  
  
    /* 운영 시간 조회, 변경 API */    
    @GetMapping("/operation-time")  
    public void getOperationTime() {  
        eventService.getOperationTime();  
    }  
  
    // 운영 시작 시간 변경  
    @PatchMapping("/open-time")  
    public void updateOpenTime(@RequestParam String openTime) {  
        eventService.setOpenTime(openTime);  
    }  
  
    // 운영 종료 시간 변경  
    @PatchMapping("/close-time")  
    public void updateCloseTime(@RequestParam String closeTime) {  
        eventService.setCloseTime(closeTime);  
    }  
  
    // Relay URL 변경  
    @PatchMapping("/relay")  
    public void updateRelayUrl(@RequestParam String url) {  
        eventService.setRelayUrl(url);  
    }  
}
```

<br>

**RecycleFn**

서비스 클래스들에서 공통으로 쓰이는 로직들을 모아놓은 클래스입니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleFn {

    private final EventRepository eventRepository;

    public void autoUpdateStatus(Event event) {
        if (event != null) {
            if (event.getOccupancy() <= 9) {
                event.setStatus(Status.LOW);
            } else if (event.getOccupancy() >= 10 && event.getOccupancy() <= event.getMaxCount()) {
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
```