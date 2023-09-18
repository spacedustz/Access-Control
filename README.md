# Access-Control
특정 공간을 카메라로 감시하며, 최대 인원 제한을 설정하고 사람이 들어가고 나갈때 자동문을 열어줍니다.
사람이 들어갈때마다 카운트 수를 실시간으로 증가시키고 화면에 최대 인원에 도달 했을때와 도달 하지 않았을때 UI, 인원 수 수정 로직을 작성합니다.

<br>

**UI에 표시해야 할 데이터**
- 현재 방안의 인원
- 최대 수용 가능 인원

<br>

**예상 구현 흐름**
1. 특정 공간에 실시간 카메라 존재
2. 사람 출입 시 딥러닝 엔진에서 이벤트 발생 (TripWire Crossing)
3. 영상에서 나온 MQTT 이벤트 데이터를 RabbitMQ의 Exchange를 거쳐 맞는 Routing Key를 가진 Quorum Queue로 데이터 쌓기
4. Quorum Queue에 쌓인 데이터를 Spring에서 가져와 필요한 필드(count 등)을 뽑아 엔티티화 -> DB 저장
5. Restful API & WebSocket을 통해 프론트엔드로 데이터 전달
6. 프론트엔드에서 데이터를 받아 현재 방안의 인원을 State로 만들어 실시간으로 인원수를 카운팅 합니다.

<br>

**구현 조건**
- 출입 시간 <-> 현재 시간 비교해서 영업 시간이 아닌 경우 Event Trigger 중지, Door 오픈 X
- 운영 시간이 아닐때 Batch 작업 중지, UI에 영업중단 표시, 자동문 API에 문열림 방지 Request 보내기
- "운영 시간이 아닐 시" DB내 현재 Count Reset
- 15명 이상일때 `만실입니다.` 로 표시- 재실 인원 보정 변수 고장 남
- 운영 시간이 아닐 때 `현재는 운영 시간이 아닙니다.` 로 표시
- Tripwire Direction In/Out별 Count 수 집계


---

## 기술 스택
- Spring Batch
- Spring Web
- Spring Data JPA
- Lombok
- WebSocket (STOMP)
- RabbitMQ (AMQP)
- H2

---

## 시퀀스 다이어그램
![시퀀스 다이어그램](https://github.com/spacedustz/Access-Control/blob/main/Description/Diagram.png)

---

## RabbitMQ Receiver

[My Github Repository](https://github.com/spacedustz/Access-Control)

<br>

**흐름**
- RabbitMQ에서 MQTT 데이터를 Queue에 쌓습니다.
- Spring Boot에서 Queue에 쌓인 메시지를 가져옵니다.
- 가져올때 해당 데이터의 구조에 맟춰서 DTO를 작성해줍니다.
- 메시지를 받을 Receiver를 작성할 때 파라미터로 넣어주면, 내부적으로 Bean으로 주입한 MessageConverter가 데이터를 변환해서 DTO에 담아줍니다.
- DTO에 담긴 데이터를 엔티티화 해서 DB에 저장합니다.
- MQTT의 필드 중 출입 Direction 필드를 빼서 출/입 변수를 만들어 엔티티화해서 저장해줍니다.
- DB에 저장한 데이터를 브라우저와 소켓 통신을 해서 실시간으로 값의 변화를 출력합니다.

---
## 구현

> 📘 **application.yml**

RabbitMQ, JPA, H2 등등 설정 파일 구성

```yaml
server:  
  servlet:  
    encoding:  
      charset: UTF-8  
      force-response: true  
  port: 8090  
  
spring:  
  # H2 설정  
  h2:  
    console:  
      enabled: true  
      path: /h2  
  datasource:  
    url: jdbc:h2:file:E:\Data\H2\H2  
    username: root  
    password: 1234  
  
  # JPA 설정  
  jpa:  
    open-in-view: false  
    hibernate:  
      ddl-auto: update  
    show-sql: false  
    properties:  
      hibernate:  
        format_sql: true  
  
  # RabbitMQ 설정  
  rabbitmq:  
    host: localhost  
    port: 5672  
    username: guest  
    password: guest  
  
# Logging  
logging:  
  level:  
    org:  
      hibernate: info
```

<br>

> 📘 **RabbitConfig**

단순히 메시지를 받을 용도로만 사용할 것이기 때문에, Queue, Exchange, Binding 함수는 빼주었습니다.

```java  
@Configuration  
public class RabbitConfig {  
    @Value("${spring.rabbitmq.host}")  
    private String host;  
  
    @Value("${spring.rabbitmq.port}")  
    private int port;  
  
    @Value("${spring.rabbitmq.username}")  
    private String id;  
  
    @Value("${spring.rabbitmq.password}")  
    private String pw;  
  
    // Message Converter Bean 주입  
    @Bean  
    MessageConverter converter() { return new Jackson2JsonMessageConverter(); }  
  
    // RabbitMQ와의 연결을 위한 Connection Factory Bean 생성  
    @Bean  
    public ConnectionFactory factory() {  
        CachingConnectionFactory factory = new CachingConnectionFactory();  
        factory.setHost(host);  
        factory.setPort(port);  
        factory.setUsername(id);  
        factory.setPassword(pw);  
  
        return factory.getRabbitConnectionFactory();  
    }  
  
    // Rabbit Template 생성  
    @Bean  
    RabbitTemplate template(org.springframework.amqp.rabbit.connection.ConnectionFactory factory, MessageConverter converter) {  
        RabbitTemplate template = new RabbitTemplate(factory);  
        template.setMessageConverter(converter);  
  
        return template;  
    }  
  
    // Subscribe Listener  
    @Bean  
    SimpleRabbitListenerContainerFactory listener(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {  
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();  
        factory.setConnectionFactory(connectionFactory);  
        factory.setMessageConverter(converter());  
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);  
  
        return factory;  
    }  
}
```  

<br>  

> 📘 **EventDTO : MQTT 데이터를 담을 용도**

RabbitMQ의 Queue에 쌓인 데이터를 임시로 담아 엔티티화 하기 위한 DTO입니다.

원본 데이터의 계층 구조에 맞게 생성해서 담아줍니다.

```java  
@Getter  
public class EventDTO {  
    private String system_date;  
    private List<Event> events;  
  
    @Getter  
    public static class Event {  
        private Extra extra;  
    }  
  
    @Getter  
    public static class Extra {  
        private String crossing_direction;  
    }  
}
```  

<br>  

> 📘 **Event : 화면에 출력할 필요한 필드만 모아서 만든 JPA Entity**

```java
@Entity  
@Getter @Setter  
@NoArgsConstructor(access = AccessLevel.PROTECTED)  
public class Event {  
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Long id;  
    @Setter  
    private int inCount = 0;  
  
    @Setter  
    private int outCount = 0;  
  
    private LocalDateTime eventTime;  
  
    private Event(LocalDateTime eventTime) {  
        this.eventTime = eventTime;  
    }  
  
    // 생성자 - 정적 팩토리 함수  
    public static Event createOf(LocalDateTime eventTime) {  
        return new Event(eventTime);  
    }  
  
    // 하루가 지날때 마다 count 수 초기화  
    public void initializeCount() {  
        this.inCount = 0;  
        this.outCount = 0;  
    }  
}
```

<br>

> 📘 **RabbitTopicListener : RabbitMQ 데이터 수신**

RabbitMQ의 Queue에 쌓인 데이터를 `@RabbitListener`를 사용해서 가져옵니다.

가져올 때, RabbitConfig에서 작성한 MessageConverter에 의해 내부적으로 데이터를 변환시켜 DTO에 저장합니다.
- MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.
- 이 String 타입 날짜 데이터를 "년-월-일T시-분-초" 형식의 (ISO 8601 규약) LocalDateTime 타입으로 변환해서 엔티티화 합니다.

```java  
@Slf4j  
@Service  
@RequiredArgsConstructor  
public class RabbitTopicListener {  
  
    private EventRepository eventRepository;  
  
    // MQTT 데이터에서 들어오는 system_date의 날짜 형식은 "EEE MMM dd HH:mm:ss yyyy" 입니다.  
    // 이 String 타입 날짜 데이터를 "년-월-일T시-분-초"의 LocalDateTime으로 변환해서 엔티티화 합니다.  
    @RabbitListener(queues = "q.frame")  
    public void receive(EventDTO message) {  
        log.info("원본 Date: " + message.getSystem_date());  
        log.info("원본 Count: " + message.getEvents().stream().map(it -> it.getExtra().getCrossing_direction()).toList());  
  
        // 원본 데이터의 system_date 필드 변환  
        String originalDate = message.getSystem_date();  
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:s yyyy", Locale.ENGLISH);  
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);  
        log.info("날짜 타입 변환 테스트 : " + convertedDate);  
          
    }  
} 
```  

<br>

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-time.png)

<br>  

> 📘 **InitSchemaLoader : 초기 데이터 설정**

* 첫번째 if 문 : DB에 데이터가 하나도 없으면 초기 데이터 생성
* 두번째 if 문 : DB에 객체가 1개 이상이고, 데이터의 날짜가 `오늘 날짜가 아닐 때` 오늘 날짜에 해당하는 객체 새로 생성

```java  
/**  
 * @author: 신건우  
 * @desc  
 * 첫번째 if 문 : DB에 데이터가 하나도 없으면 초기 데이터 컬럼 생성  
 * 두번째 if 문 : DB에 객체가 1개 이상이고, 데이터의 날짜가 오늘 날짜가 아닐때 오늘 날짜에 해당하는 객체 새로 생성  
 */  
@Slf4j  
@Component  
@RequiredArgsConstructor  
public class InitSchemaLoader implements ApplicationRunner {  
  
    private final EventRepository eventRepository;  
  
    @Override  
    public void run(ApplicationArguments args) throws Exception {  
  
        // 테이블에 데이터 수 확인  
        long objectCount = eventRepository.count();  
  
        // DB에 데이터가 하나도 없으면 초기 데이터 생성  
        if (objectCount == 0) {  
            log.info("Event Table 내부에 데이터가 없습니다. 객체를 생성합니다.");  
  
            Event event = Event.createOf(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));  
  
            try {  
                eventRepository.save(event);  
                log.info("객체 생성 완료, Event ID: {}", event.getId());  
            } catch (Exception e) {  
                log.error("객체 생성 실패", e);  
                throw new CommonException("INIT-001", HttpStatus.INTERNAL_SERVER_ERROR);  
            }  
        }  
  
        // DB에 데이터가 1개 이상이고, 그 데이터의 현재 년월일이 현재 년월일과 맞지 않으면 새로운 객체 생성  
        if (objectCount > 0) {  
            log.info("Event Table 내부에 데이터가 있습니다. 객체의 날짜가 현재 날짜와 동일한 지 검증 중...");  
  
            Event storedEvent = null;  
            try {  
                storedEvent = eventRepository.findById(1L).orElse(null);  
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
                    eventRepository.save(event);  
                    log.info("기존 데이터의 날짜와 현재 시간이 불일치합니다, 새로운 객체를 생성 합니다. - Event ID: {}", event.getId());  
                } catch (Exception e) {  
                    log.error("기존 데이터의 날짜와 현재 시간 불일치합니다, - 객체 생성 실패", e);  
                    throw new CommonException("INIT-003", HttpStatus.INTERNAL_SERVER_ERROR);  
                }  
            } else {  
                log.info("현재 날짜의 데이터가 이미 존재합니다, 객체 생성 중지 - 현재 데이터의 날짜 : {}", storedEvent.getEventTime().format(formatter));  
            }  
        }  
    }  
}
```

<br>

**DB에 데이터가 하나도 없을때**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-initdata.png)

<br>

**DB에 현재 날짜에 해당하는 데이터가 있을 때**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-initdata2.png)

<br>

**현재 데이터는 있지만, 날짜가 다를때 새로운 데이터 생성**

- 임의로 Update 쿼리를 써서 날짜만 변경해서 테스트
- 현재 날짜와 불일치 하는 데이터를 발견하면 오늘 날짜로 새로운 데이터 생성

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-initdata3.png)

<br>

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-initdata4.png)