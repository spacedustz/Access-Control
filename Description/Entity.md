## 📘 **EventDTO : MQTT 데이터를 담을 용도**

RabbitMQ의 Queue에 쌓인 데이터를 임시로 담아 엔티티화 하기 위한 DTO입니다.

원본 이벤트 데이터(MQTT - Json)에서 필요한 필드 2개만 뽑아 Json 계층 구조에 맞게 생성해서 담아줍니다.
- system_date
- crossing_direction

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

## 📘 **Event : 화면에 출력할 필요한 필드만 모아서 만든 JPA Entity**

이 엔티티에서 사용할 필드를 설명하겠습니다.

<br>

eventTime : 딥러닝 엔진에서 트리거가 발동되서 나온 데이터의 현재 시간 **(YYYY-MM-DDTHH-mm-ss 형식으로 변환하어 저장)**
Incount : 입장한 사람 수 카운트 (내부 카운팅용 로직)
OutCount : 퇴장한 사람 수 카운트 (내부 카운팅용 로직)
Occupancy : 현재 내부 인원 값 (InCount - OutCount 값)
MaxCount : 입장 가능한 최대 인원 수 (단순히 화면에 출력될 값, 변경 가능)
relayUrl : Door API의 URL
state : 현재 인원에 따라 내부의 상태를 출력할 값

```java
@Entity  
@Getter @Setter  
@NoArgsConstructor(access = AccessLevel.PROTECTED)  
public class Event {  
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Long id;  
  
    private LocalDateTime eventTime; // 이벤트 시간  
    @Setter  
    private int inCount = 0; // 입장 카운트  
    @Setter  
    private int outCount = 0; // 퇴장 카운트  
    @Setter  
    private int occupancy = 0; // 현재 Room 내 인원 수 : InCount - OutCount  
    @Setter  
    private int maxCount = 15; // 최대 수용 인원  
  
    private String relayUrl = ""; // Relay URL  
    @Setter  
    private String status = "입장 가능합니다."; // Room 상태
  
    private Event(LocalDateTime eventTime) {  
        this.eventTime = eventTime;  
    }  
  
    // 생성자 - 정적 팩토리 함수  
    public static Event createOf(LocalDateTime eventTime) {  
        return new Event(eventTime);  
    }  
}
```
