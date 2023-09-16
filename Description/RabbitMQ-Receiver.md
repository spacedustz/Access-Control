## RabbitMQ Receiver

**흐름**
- RabbitMQ에서 MQTT 데이터를 Queue에 쌓습니다.
- Spring Boot에서 Queue에 쌓인 메시지를 가져옵니다.
- 가져올때 해당 데이터의 구조에 맟춰서 DTO를 작성해줍니다.
- 메시지를 받을 Receiver를 작성할 때 파라미터로 넣어주면, 내부적으로 Bean으로 주입한RabbitConverter가 데이터를 변환해서 DTO에 담아줍니다.
- DTO에 담긴 데이터를 엔티티화 해서 DB에 저장합니다.
- DB에 저장한 데이터를 브라우저와 소켓 통신을 해서 실시간으로 값의 변화를 출력합니다.

<br>

**RabbitConfig**

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

**EventDTO**

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
        private int count;
    }
}
```

<br>

**RabbitTopicListener**

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

    @RabbitListener(queues = "test.queue")
    public void receive(EventDTO message) {
        log.info("Received Date: " + message.getSystem_date());
        log.info("Received Count: " + message.getEvents().stream().map(it -> it.getExtra().getCount()).toList());

        // 원본 데이터의 system_date 필드 변환
        String originalDate = message.getSystem_date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:s yyyy", Locale.ENGLISH);
        LocalDateTime convertedDate = LocalDateTime.parse(originalDate, formatter);
        log.info("날짜 타입 변환 테스트 : " + convertedDate);

        // DTO -> Entity -> Repository
        Event event = Event.createOf(convertedDate);
        event.setCount(event.getCount() + 1);
    }
}
```