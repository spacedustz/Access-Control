## 📘 **ScheduleTask : 초기 데이터 생성, 주기적인 Schedule Task 작업**

매일 00시 00분 01초에 Scehdule을 이용하여 매일 00시 00분에, 테이블에 현재 날짜 값을 가진 데이터가 없으면,

자동으로 현재 날짜의 데이터를 생성하게 하는 클래스입니다.

<br>

* scheduleTask() 함수 : rabbitTopicListener의 엔티티 검증 후 생성하는 로직을 매일 1번 수행합니다.
* healthCheck() 함수 : 10초 마다 운영시간인지 확인 후, 운영시간이 아니면 객체의 Status 상태를 변화 후 소켓에 전송

```java  
@Slf4j  
@Component  
@RequiredArgsConstructor  
public class ScheduleTask {  
    private final EventRepository eventRepository;  
    private final RecycleFn recycleFn;  
    private final SimpMessagingTemplate template;  
    private final RabbitTopicListener rabbitTopicListener;  
  
    @Scheduled(cron = "1 0 0 * * *", zone = "Asia/Seoul")  
    public void scheduleTask() throws Exception {  
        rabbitTopicListener.createEntity();  
        log.info("데이터 생성 태스크 실행 - 시간 : {}", LocalDateTime.now());  
    }  
  
    // 10초 마다 운영시간인지 체크해서 현황판의 Status를 변화 시키는 Scheduler    @Scheduled(cron = "0/5 * * * * *")  
    public void healthCheck() {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            recycleFn.validateOperationTime(event);  
            eventRepository.save(event);  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            log.error("객체 조회 실패", e);  
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