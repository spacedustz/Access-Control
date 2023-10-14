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
    private String customStatus = ""; // 임의로 상태를 변경하고 싶을때 사용할 변수  

    @Setter
    private String openTime = "09:00";

    @Setter
    private String closeTime = "18:00";

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
    HIGH("만실입니다."),
    NOT_OPERATING("운영시간이 아닙니다.");

    @Getter
    private final String desc;

    Status(String desc) {
        this.desc = desc;
    }
}
```