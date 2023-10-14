## 📘 **DTO : MQTT 데이터를 담을 용도**

RabbitMQ의 Queue에 쌓인 데이터를 임시로 담아 엔티티화 하기 위한 DTO입니다.

원본 이벤트 데이터(MQTT - Json)에서 필요한 필드 2개만 뽑아 Json 계층 구조에 맞게 생성해서 담아줍니다.
- system_date
- crossing_direction

<br>

> 📌 **EventDto**

Response 내부 클래스는 웹 브라우저에서 Spring 웹 소켓에 요청을 보내고 내부 로직을 거쳐,

데이터를 다시 브라우저로 반환 해줄때 필요한 필드들 입니다.

```java  
// RabbitMQ에서 들어오는 데이터를 받을 용도  
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

> 📌 **InstanceDto**

감시 서버의 카메라 인스턴스의 상태 정보를 역직렬화 하기 위한 클래스입니다.

상태를 받는 이유는, 별도의 Health Check 스레드를 만들어, 인스턴스가 죽어도 다시 올라오게 하기 위함입니다.

```java
@Data  
@JsonInclude(JsonInclude.Include.NON_NULL)  
public class InstanceDto {  
    @JsonProperty("instance_name")  
    private String instanceName;  
  
    @JsonProperty("solution")  
    private String solution;  
  
    @JsonProperty("solution_name")  
    private String solutionName;  
  
    @JsonProperty("solution_path")  
    private String solutionPath;  
  
    @JsonProperty("solution_version")  
    private String solutionVersion;  
  
    @JsonProperty("state")  
    private int state;  
}
```