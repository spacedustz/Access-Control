> 📘 **EventService & WebSocketController**

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
```

<br>

**WebSocketController**

```java
@RestController  
@RequestMapping("/ws")  
@RequiredArgsConstructor  
public class WebSocketController {  
    private final EventService eventService;  
  
    // Event 객체 Status 값 변경 API    @PatchMapping("/update-status")  
    public ResponseEntity<String> updateStatus(@RequestParam String status) {  
        return new ResponseEntity<>(eventService.updateStatus(status), HttpStatus.OK);  
    }  
  
    // Event 객체 MaxCount 값 변경 API    @PatchMapping("/update-max")  
    public ResponseEntity<EventDTO.Response> updateMaxCount(@RequestParam String max) {  
        return new ResponseEntity<>(EventDTO.Response.fromEntityForUpdateMaxCount(eventService.updateMaxCount(max)), HttpStatus.OK);  
    }  
  
    // 초기 데이터 로드용 API    @GetMapping("/init")  
    public ResponseEntity<EventDTO.Response> getInitData() {  
        return new ResponseEntity<>(EventDTO.Response.fromEntity(eventService.getInitData()), HttpStatus.OK);  
    }  
}
```