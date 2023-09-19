> 📘 **RabbitTopicListener : RabbitMQ 데이터 수신**

RabbitMQ의 Queue에 쌓인 데이터를 `@RabbitListener`를 사용해서 가져옵니다.

가져올 때, RabbitConfig에서 작성한 MessageConverter에 의해 내부적으로 데이터를 변환시켜 DTO에 저장합니다.
- MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
- 이 String 타입 날짜 데이터를 "년-월-일T시-분-초" 형식의 (ISO 8601 규약) LocalDateTime 타입으로 변환해서 엔티티화 합니다.
- RestTemplate을 이용해 Door API를 호출해 조건을 통과하면 문을 열어줍니다.

```java  
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {

    @Value("operation.open-time")
    private final String openTime; // 운영 시작 시간

    @Value("operation.close-time")
    private final String closeTime; // 운영 종료 시간

    private final String currentDate = String.valueOf(LocalDate.now());

    private EventRepository eventRepository;

    @Cacheable("entityCount")
    public Long getEntityCount() {
        return eventRepository.count();
    }

    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.
    @RabbitListener(queues = "q.frame")
    public void receive(EventDTO message) {
        log.info("원본 Date: {}", message.getSystem_date());
        log.info("원본 Count: " + message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList());

        // 엔티티 조회
        Event event = eventRepository.findById(getEntityCount()).orElseThrow(() -> new CommonException("DATA-001 : 엔티티 조회 실패", HttpStatus.NOT_FOUND));

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

        // 이벤트로 넘어온 데이터의 Direction 가져오기
        String direction = message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList().get(0);

        // 엔티티 YYYY-MM-DD 시간 검증
        if (!entityYMDDate.equals(currentDate)) {
            throw new CommonException("TIME-001 : 데이터의 현재 시간과 맞지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 현재 Entity와 이벤트로 넘어온 년월일이, 현재 시간과 맞는지 검증
        if (!eventYMDDate.equals(currentDate)) {
            log.error("이벤트 데이터의 날짜가 현재 날짜와 맞지 않습니다. - 현재 날짜 : {}, 데이터의 날짜 : {}", currentDate, entityHMDate);
            throw new CommonException("TIME-001", HttpStatus.BAD_REQUEST);
        }

        if (!entityHMDate.equals(openTime) && !entityHMDate.equals(closeTime)) {
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, entityHMDate);
            throw new CommonException("TIME-002", HttpStatus.BAD_REQUEST);
        }

        while (event.getInCount() < 15 && event.getOutCount() < 15) {
            
            if (direction.equalsIgnoreCase("down")) {
                event.setInCount(event.getInCount() + 1);
            } else if (direction.equalsIgnoreCase("up")) {
                event.setOutCount(event.getOutCount() + 1);
            }
        }
    }

    // 년-월-일 변환기
    public String ymdFormatter(@Nullable LocalDateTime dateTime) {
        DateTimeFormatter YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTime.format(YMDFormatter);
    }

    // 시-분 변환기
    public String hmFormatter(@Nullable LocalDateTime date) {
        DateTimeFormatter HMFormatter = DateTimeFormatter.ofPattern("hh-mm");
        return date.format(HMFormatter);
    }
}

```  

<br>

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-time.png)
