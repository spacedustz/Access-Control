## 📘 **Event Service & Controller & RecycleFn**

> 📌 **EventService**

EventService 의 모든 함수는 반환값이 없는 void이며, 값이 변경된 객체를 저장하고 소켓을 통해 프론트엔드로 전달합니다.

- updateCustomStatus : Enum에 정해진 상태값이 아닌 "고장입니다" 등의 Custom Status를 사용하기 위한 업데이트 함수
- updaMaxCount : 방안의 최대 입장 인원을 변경하는 함수
- increaseOccupancy : 현재 방안의 인원 수를 임의로 증가 시키기 위한 함수
- decreaseOccupancy : 현재 방인의 인원 수를 임의로 감소 시키기 위한 함수
- setOpenTime : 운영 시작 시간을 변경하는 함수
- setCloseTime : 운영 종료 시간을 변경하는 함수
- getOperationTime : 현재 운영시간을 반환하는 함수
- setRelayUrl : Door API URL을 설정하는 함수
- getInitData() : 서버 초기 로드 시 데이터를 불러와 화면에 띄우는 용도의 함수

```java
@Slf4j  
@Service  
@Transactional  
@RequiredArgsConstructor  
public class EventService {  
    private final EventRepository eventRepository;  
    private final SimpMessagingTemplate template;  
    private final RecycleFn recycleFn;  
  
    // Event 객체의 Status 값 업데이트  
    public void updateCustomStatus(String status) {  
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
    }  
  
    // maxCount 값 업데이트  
    public void updateMaxCount(String max) {  
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
    public void getInitData() {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            recycleFn.autoUpdateStatus(event);  
            recycleFn.validateOperationTime(event);  
            eventRepository.save(event);  
            log.info("Event 객체 데이터 로드 완료 - Event ID : {}", event.getId());  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            log.error("Event 객체 데이터 로드 실패", e);  
        }  
    }  
}
```

<br>

> 📌 **EventController**

Controller도 전부 반환값이 없는 void 입니다.

브라우저에서 소켓에 접속해 백엔드인 Spring으로 부터 각종 데이터를 받아오기 위한 Rest API를 작성합니다.

두 클래스 다 값 조회, 변경의 간단한 API 이므로 설명은 생략하겠습니다.

```java
@RestController  
@RequestMapping("/ws")  
@RequiredArgsConstructor  
public class EventController {  
    private final EventService eventService;  
  
    // Event 객체 Status 값 변경 API    
    @PatchMapping("/update-status")  
    public void updateStatus(@RequestParam String status) {  
        eventService.updateCustomStatus(status);  
    }  
  
    // Event 객체 MaxCount 값 변경 API    
    @PatchMapping("/update-max")  
    public void updateMaxCount(@RequestParam String max) {  
        eventService.updateMaxCount(max);  
    }  
  
    // 초기 데이터 로드용 API    
    @GetMapping("/init")  
    public void getInitData() {  
        eventService.getInitData();  
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

> 📌 **RecycleFn**

비즈니즈 로직에서 공통으로 쓰이는 함수들을 모아 놓은 클래스입니다.

- @Cacheable getEntity, getEnttyCount : 객체를 조회하고 캐싱하는 함수
- initCount : 단순히 객체의 모든 Count 값을 초기화 하는 함수
- timeFormatter : 년월일 / 시분을 변환해 문자열로 반환하는 함수
- autoUpdateStatus : 현재 인원에 따른 객체 상태 업데이트
- validateOperation Time : 현황판의 운영 상태 검증
- validateOccupancy : 현재 인원의 비정상 카운팅 검증
- validateOperatingStatus : 들어오는 이벤트의 시간이 운영시간에 해당하는지 검증

```java
@Slf4j  
@Service  
@Transactional  
@RequiredArgsConstructor  
public class RecycleFn {  
    private final EventRepository eventRepository;  
    private final SimpMessagingTemplate template;  
    private String currentDate = String.valueOf(LocalDate.now());  
  
    @Cacheable("entityCount")  
    public Long getEntityCount() {  
        return eventRepository.count();  
    }  
  
    @Cacheable("entity")  
    public Event getEntity(Long pk) {  
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));  
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
  
    // 현재 인원에 따른 객체 상태 업데이트  
    public void autoUpdateStatus(Event event) {  
        if (event.getCustomStatus().equals("")) {  
            if (event.getOccupancy() <= 9) {  
                event.setStatus(Status.LOW);  
            } else if (event.getOccupancy() >= 10 && event.getOccupancy() < event.getMaxCount()) {  
                event.setStatus(Status.MEDIUM);  
            } else if (event.getOccupancy() >= event.getMaxCount()) {  
                event.setStatus(Status.HIGH);  
            }  
        } else {  
            log.info("현재 방안의 상태 - {}", event.getCustomStatus());  
        }  
    }  
  
    // 운영 시간 검증 함수  
    public Event validateOperationTime(Event event) {  
        String openTime = event.getOpenTime();  
        String closeTime = event.getCloseTime();  
  
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");  
  
        LocalDateTime nowTime = LocalDateTime.now();  
  
        LocalTime now = LocalTime.parse(hmFormatter(nowTime));  
  
        // openTime과 closeTime을 LocalDateTime으로 변환  
        LocalTime open = LocalTime.parse(openTime, timeFormatter);  
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);  
  
        // 운영 시간 검증  
        if (now.isAfter(open) && now.isBefore(close)) {  
            log.info("정상 운영 시간 입니다. [ 운영 시간 ] {} - {}", open, close);  
            autoUpdateStatus(event);  
        } else {  
            event.setStatus(Status.NOT_OPERATING);  
            initiateCount(event);  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 현재 시간 : {}", openTime, closeTime, now);  
        }  
  
        return event;  
    }  
  
    // 재실 인원 검증 함수  
    public void validateOccupancy(Event event) {  
        try {  
            if (event.getOccupancy() < 0) {  
                initiateCount(event);  
                eventRepository.save(event);  
  
                log.error("재실 인원 오류 - In/Out Count, Occupancy 초기화 - 초기화 된 Occupancy 값 : {}", event.getOccupancy());  
            }  
  
            if (event.getOccupancy() >= event.getMaxCount()) {  
                log.info("인원 초과 - 재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
            }  
  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId(), e);  
        }  
    }  
  
    // 운영시간 검증 함수  
    public void validateOperatingStatus(String entityYMDDate,  
                                        String eventYMDDate,  
                                        LocalTime open,  
                                        LocalTime close,  
                                        LocalTime eventDateTime,  
                                        String openTime,  
                                        String closeTime,  
                                        Event event) {  
  
        // 이벤트 데이터의 날짜 검증  
        if (!eventYMDDate.equals(currentDate) || (!entityYMDDate.equals(currentDate))) {  
            log.error("데이터의 날짜가 오늘 날짜가 아닙니다. - 현재 날짜 : {}, 데이터의 날짜 : {}", currentDate, eventYMDDate);  
        }  
  
        // 이벤트 데이터의 운영 시간 검증  
        if (!eventDateTime.isAfter(open) && !eventDateTime.isBefore(close)) {  
            event.setStatus(Status.NOT_OPERATING);  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, eventDateTime);  
        }  
  
        try {  
            eventRepository.save(event);  
        } catch (Exception e) {  
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());  
        }  
  
        // Web Socket Session에 Event 객체 전달  
        template.convertAndSend("/count/data", event);  
    }  
}
```