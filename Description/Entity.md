## 📘 **EventDTO : MQTT 데이터를 담을 용도**

RabbitMQ의 Queue에 쌓인 데이터를 임시로 담아 엔티티화 하기 위한 DTO입니다.

원본 이벤트 데이터(MQTT - Json)에서 필요한 필드 2개만 뽑아 Json 계층 구조에 맞게 생성해서 담아줍니다.
- system_date
- crossing_direction

<br>

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
  
    // Event 객체 Response 용도  
    @Getter  
    @AllArgsConstructor(access = AccessLevel.PRIVATE)  
    public static class Response {  
        private Long id; // ID  
        private int occupancy; // 현재 Room 내 인원 수 : InCount - OutCount        
        private int maxCount; // 최대 수용 인원  
        private String customStatus; // Room 상태  
        private String relayUrl; // Relay URL  
  
        private Response(int maxCount) {  
            this.maxCount = maxCount;  
        }  
  
        private Response(Long id, int occupancy, int maxCount, String customStatus) {  
            this.id = id;  
            this.occupancy = occupancy;  
            this.maxCount = maxCount;  
            this.customStatus = customStatus;  
        }  
  
        // 현황판용 응답 객체  
        public static EventDTO.Response fromEntityForViewer(com.accesscontrol.entity.Event entity) {  
            return new EventDTO.Response(  
                    entity.getId(),  
                    entity.getOccupancy(),  
                    entity.getMaxCount(),  
                    entity.getStatus().getDesc()  
            );  
        }  
  
        // 관리자 페이지용 응답 객체  
        public static EventDTO.Response fromEntityForAdmin(com.accesscontrol.entity.Event entity) {  
            return new EventDTO.Response(  
                    entity.getId(),  
                    entity.getOccupancy(),  
                    entity.getMaxCount(),  
                    entity.getStatus().getDesc(),  
                    entity.getRelayUrl()  
            );  
        }  
  
        // maxCount 업데이트 용  
        public static EventDTO.Response fromEntityForUpdateMaxCount(com.accesscontrol.entity.Event entity) {  
            return new EventDTO.Response(  
                    entity.getMaxCount()  
            );  
        }  
    }  
}
```  
  
---

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

<br>

**@EntityListeners(WebSocketEntityListener.class)**
- 엔티티의 값 변화 이벤트를 트리거로 사용해서, 웹 브라우저에 소켓으로 변화된 데이터값을 전달할 목적의 Annotation

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
    @Enumerated(EnumType.STRING)  
    private Status status = Status.LOW; // Room 상태 기본값 : 입장 가능합니다.  
  
    @Setter  
    private String customStatus = "";  // 임의로 상태를 변경하고 싶을때 사용할 변수
  
    private Event(LocalDateTime eventTime) {  
        this.eventTime = eventTime;  
    }  
  
    // 생성자 - 정적 팩토리 함수  
    public static Event createOf(LocalDateTime eventTime) {  
        return new Event(eventTime);  
    }  
}
```

<br>

**Status Enum**

```java
// 현재 방안의 상태  
public enum Status {  
    LOW("입장 가능합니다."),  
    MEDIUM("혼잡합니다."),  
    HIGH("만십입니다."),  
    NOT_OPERATING("운영시간이 아닙니다.");  
  
    @Getter  
    private final String desc;  
  
    Status(String desc) {  
        this.desc = desc;  
    }  
}
```