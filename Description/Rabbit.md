## 📘 **RabbitConfig**

직접 큐 등을 만들지 않고 만들어져 있는 Queue에서 단순히 메시지를 받을 용도의 함수들만 사용할 것이기 때문에

Queue 생성, Exchange 생성, Binding 함수는 빼주었습니다.

<br>

`factory()` : RabbitMQ와의 AMQP 연결을 위한 커넥션을 설정하는 함수입니다.

`converter()` : RabbitMQ의 메시지를 내부적으로 변환할 메시지 컨버터를 Bean으로 주입합니다.

`template()` : RabbitMQ의 데이터가 내부 컨버터를 거쳐 어떤 형식으로 받게 될지 정하는 데이터를 담는 그릇 같은 존재입니다.

`listener()` : RabbitMQ set커넥션, set컨버터를 설정해서 ListenerContainerFactory를 정의합니다.
- `setAcknowleggeMode`는 RabbitMQ의 Queue가 Quorum Queue이기 때문에 설정했습니다.
- Quorum Queue는 메시지를 받으면 메시지를 받았다는 ACK를 날려야 하는데 그걸 자동으로 해주는 옵션입니다.

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
  
        return factory;  
    }  
  
    // Rabbit Template 생성  
    @Bean  
    RabbitTemplate template() {  
        RabbitTemplate template = new RabbitTemplate(factory());  
        template.setMessageConverter(converter());  
  
        return template;  
    }  
  
    // Subscribe Listener  
    @Bean  
    SimpleRabbitListenerContainerFactory listener() {  
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();  
        factory.setConnectionFactory(factory());  
        factory.setMessageConverter(converter());  
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);  
  
        return factory;  
    }  
}
```  
