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
