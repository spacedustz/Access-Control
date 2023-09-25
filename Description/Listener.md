## 📘 **RabbitTopicListener : RabbitMQ 데이터 수신**

RabbitMQ의 Queue에 쌓인 데이터를 `@RabbitListener`를 사용해서 가져옵니다.

가져올 때, RabbitConfig에서 작성한 MessageConverter에 의해 내부적으로 데이터를 변환시켜 DTO에 저장합니다.
- MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
- 이 String 타입 날짜 데이터를 "년-월-일T시-분-초" 형식의 (ISO 8601 규약) LocalDateTime 타입으로 변환해서 엔티티화 합니다.

<br>

**설명**

- 이벤트 데이터의 시간을(LocalDateTime)을 `(년-월-일)` 형식으로 분할하고 확인해 데이터의 날짜가 현재 엔티티의 날짜와 맞지 않으면 로그를 찍습니다.
- 이벤트 데이터의 시간(LocalDateTime)을`(시-분-초)` 형식으로 분할하고 확인해 운영 시간(09:00 ~ 18:00)이 아니면 Door Open API 요청을 안보냅니다.
- Incount : 입장한 사람 수 카운트 (내부 카운팅용 로직)
- OutCount : 퇴장한 사람 수 카운트 (내부 카운팅용 로직)
- Occupancy : 현재 내부 인원 값 (InCount - OutCount 값)
- MaxCount : 입장 가능한 최대 인원 수 (단순히 화면에 출력될 값, 변경 가능)

<br>

**현재 내부 인원의 수 : Occupancy**
- 최대 인원을 초과할 수 있으며, 인원이 초과해도 일단 문을 열어줍니다. 문을 열되 만실임을 알려줍니다.

<br>

**`template.convertAndSend("/count/data", event)` 부분**
- 로직 마지막의 이 부분은 브라우저에서 접속할 WebSocket의 Subscribe URL입니다.
- 메시지가 넘어올때마다 변환, 비교, 검증을 한 후 객체를 소켓으로 내보냅니다.


```java  
@Slf4j  
@Service  
@RequiredArgsConstructor  
public class RabbitTopicListener {  
  
    @Value("${operation.open-time}")  
    private String openTime; // 운영 시작 시간  
  
    @Value("${operation.close-time}")  
    private String closeTime; // 운영 종료 시간  
  
    private String currentDate = String.valueOf(LocalDate.now());  
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
  
    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.  
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.  
    @RabbitListener(queues = "q.frame")  
    public void receive(EventDTO message) {  
        Event event = null;  
  
        try {  
            event = getEntity(getEntityCount());  
        } catch (Exception e) {  
            log.error("DATA-001 : 엔티티 조회 실패");  
            throw new CommonException("DATA-001 : 엔티티 조회 실패", HttpStatus.NOT_FOUND);  
        }  
  
        // 원본 데이터의 system_date 필드 변환  
        String originalDate = message.getSystem_date();  
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:s yyyy", Locale.ENGLISH);  
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);  
  
        // DB에 저장된 데이터의 날짜 나누기  
        String entityYMDDate = recycleFn.ymdFormatter(event.getEventTime()); // YYYY-MM-DD  
        String entityHMDate = recycleFn.hmFormatter(event.getEventTime()); // HH-MM  
  
        // 이벤트로 넘어온 데이터의 날짜 나누기  
        String eventYMDDate = recycleFn.ymdFormatter(convertedDate); // YYYY-MM-DD  
        String eventHMDate = recycleFn.hmFormatter(convertedDate); // HH-MM  
  
        // 이벤트로 넘어온 데이터의 시간이 운영시간 범위에 존재하는지 확인하기 위한 LocalTime 타입 변환  
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");  
        LocalTime open = LocalTime.parse(openTime, timeFormatter);  
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);  
        LocalTime eventDateTime = LocalTime.parse(eventHMDate, timeFormatter);  
  
        // 현재 재실 인원이 마이너스(-)로 가는 비정상적인 상황 발생 시 in/out count, occupancy 값 초기화  
        validateOccupancy(event);  
  
        // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증  
        validateOperatingStatus(entityYMDDate, eventYMDDate, eventDateTime, open, close, openTime, closeTime, entityHMDate, event);  
  
        // 이벤트로 넘어온 데이터의 Direction 가져오기  
        List<String> directions = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();  
  
        for (String direction : directions) {  
            if (direction.equalsIgnoreCase("down")) {  
                event.setInCount(event.getInCount() + 1);  
                log.info("입장");  
            } else if (direction.equalsIgnoreCase("up")) {  
                event.setOutCount(event.getOutCount() + 1);  
                log.info("퇴장");  
            }  
  
            // 날짜, 운영시간 검증, 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증  
            validateOperatingStatus(entityYMDDate, eventYMDDate, eventDateTime, open, close, openTime, closeTime, entityHMDate, event);  
  
            // 현재 재실 인원이 마이너스(-)로 가는 비정상적인 상황 발생 시 in/out count, occupancy 값 초기화  
            validateOccupancy(event);  
  
            event.setOccupancy(event.getInCount() - event.getOutCount());  
            log.info("재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
  
            recycleFn.autoUpdateStatus(event);  
  
            eventRepository.save(event);  
  
            // Web Socket Session 에 Event 객체 전달  
            template.convertAndSend("/count/data", event);  
        }  
    }  
  
    // 재실 인원 검증 함수  
    public void validateOccupancy(Event event) {  
        if (event.getOccupancy() < 0) {  
            log.info("재실 인원 오류 - In/Out Count, Occupancy 초기화");  
            recycleFn.initiateCount(event);  
  
            try {  
                eventRepository.save(event);  
            } catch (Exception e) {  
                log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());  
            }  
        }  
  
        if (event.getOccupancy() >= 15) {  
            log.info("인원 초과 - 재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
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
                                        String entityHMDate,  
                                        Event event) {  
  
        if (!entityYMDDate.equals(currentDate) || !eventYMDDate.equals(currentDate) || (!(eventDateTime.isAfter(open) && eventDateTime.isBefore(close)))) {  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, entityHMDate);  
            recycleFn.initiateCount(event);  
            event.setStatus(Status.NOT_OPERATING);  
  
            try {  
                eventRepository.save(event);  
            } catch (Exception e) {  
                log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());  
            }  
        }  
    }  
}
```  

<br>

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-count.png)