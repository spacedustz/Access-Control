> 📘 **RabbitTopicListener : RabbitMQ 데이터 수신**

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
        String entityYMDDate = ymdFormatter(event.getEventTime()); // YYYY-MM-DD  
        String entityHMDate = hmFormatter(event.getEventTime()); // HH-MM  
  
        // 이벤트로 넘어온 데이터의 날짜 나누기  
        String eventYMDDate = ymdFormatter(convertedDate); // YYYY-MM-DD  
        String eventHMDate = hmFormatter(convertedDate); // HH-MM  
  
        // 이벤트로 넘어온 데이터의 시간이 운영시간 범위에 존재하는지 확인하기 위한 LocalTime 타입 변환  
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");  
        LocalTime open = LocalTime.parse(openTime, timeFormatter);  
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);  
        LocalTime eventDateTime = LocalTime.parse(eventHMDate, timeFormatter);  
  
        // 이벤트로 넘어온 데이터의 Direction 가져오기  
        List<String> directions = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList();  
  
        // 엔티티 YYYY-MM-DD 시간 검증  
        if (!entityYMDDate.equals(currentDate)) {  
            log.error("데이터의 현재 시간과 맞지 않습니다.");  
        }  
  
        // 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증  
        if (!eventYMDDate.equals(currentDate)) {  
            log.error("이벤트 데이터의 날짜가 현재 날짜와 맞지 않습니다. - 현재 날짜 : {}, 데이터의 날짜 : {}", currentDate, entityHMDate);  
        }  
  
        // 운영시간 검증  
        if (!(eventDateTime.isAfter(open) && eventDateTime.isBefore(close))) {  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, entityHMDate);  
        }  
  
        if (event.getOccupancy() < 0) {  
            log.error("재실 인원은 0 이하가 될 수 없습니다.");  
        }  
  
        if (event.getOccupancy() >= 15) {  
            log.info("만실 - 인원 초과입니다.");  
            log.info("재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
        }  
  
        for (String direction : directions) {  
            if (direction.equalsIgnoreCase("down")) {  
                event.setInCount(event.getInCount() + 1);  
                log.info("입장");  
            } else if (direction.equalsIgnoreCase("up")) {  
                event.setOutCount(event.getOutCount() + 1);  
                log.info("퇴장");  
            }  
  
            if (event.getOccupancy() > 15) {  
                log.info("현재 흡연실 내부가 만실입니다.");  
                event.setOccupancy(15);  
            }  
  
            if (event.getOccupancy() > 12) {  
                log.info("현재 흡연실 내부가 혼잡합니다.");  
            }  
  
            event.setOccupancy(event.getInCount() - event.getOutCount());  
            log.info("재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
            eventRepository.save(event);  
  
            // Web Socket Session 에 Event 객체 전달  
            template.convertAndSend("/count/data", event);  
        }  
    }  
  
    // 년-월-일 변환기  
    public String ymdFormatter(@Nullable LocalDateTime dateTime) {  
        DateTimeFormatter YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");  
        return dateTime.format(YMDFormatter);  
    }  
  
    // 시-분 변환기  
    public String hmFormatter(@Nullable LocalDateTime date) {  
        DateTimeFormatter HMFormatter = DateTimeFormatter.ofPattern("HH:mm");  
        return date.format(HMFormatter);  
    }  
}
```  

<br>

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-count.png)