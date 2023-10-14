## 📘 **Event Service**

EventService 의 모든 함수는 반환값이 없는 void이며, 값이 변경된 객체를 저장하고 소켓을 통해 프론트엔드로 전달합니다.

- updateCustomStatus : Enum에 정해진 상태값이 아닌 "고장입니다" 등의 Custom Status를 사용하기 위한 업데이트 함수
- updaMaxCount : 방안의 최대 입장 인원을 변경하는 함수
- increaseOccupancy : 현재 방안의 인원 수를 임의로 증가 시키기 위한 함수
- decreaseOccupancy : 현재 방인의 인원 수를 임의로 감소 시키기 위한 함수
- setOpenTime : 운영 시작 시간을 변경하는 함수
- setCloseTime : 운영 종료 시간을 변경하는 함수
- getOperationTime : 현재 운영시간을 반환하는 함수
- setRelayUrl : Door API URL을 설정하는 함수
- getInitData() : 서버 초기 로드 시 데이터를 불러와 화면에 띄우는 용도의 함수

```java
@Slf4j  
@Service  
@Transactional  
@RequiredArgsConstructor  
public class EventService {  
    private final EventRepository eventRepository;  
    private final SimpMessagingTemplate template;  
    private final RecycleFn recycleFn;  
  
    // Event 객체의 Status 값 업데이트  
    public void updateCustomStatus(String status) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setCustomStatus(status);  
            eventRepository.save(event);  
            template.convertAndSend("/count/customStatus", event);  
        } catch (Exception e) {  
            assert event != null;  
            log.error("Event 객체 Statue Update 실패 - Event ID : {}, Status 변경 여부 : {}", event.getId(), event.getStatus());  
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);  
        }  
  
        log.info("Event 객체 상태 업데이트 완료 - 상태 : {}", event.getCustomStatus());  
    }  
  
    // maxCount 값 업데이트  
    public void updateMaxCount(String max) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setMaxCount(Integer.parseInt(max));  
            recycleFn.autoUpdateStatus(event);  
            eventRepository.save(event);  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            assert event != null;  
            log.error("Event 객체 maxCount Update 실패 - Event ID : {}, maxCount 변경 여부 : {}", event.getId(), event.getMaxCount());  
            throw new CommonException("EVENT-001", HttpStatus.BAD_REQUEST);  
        }  
  
        log.info("Event 객체 최대 인원 업데이트 완료 - 최대 인원 : {}", event.getMaxCount());  
    }  
  
    // 재실 인원 값 증가 함수  
    public void increaseOccupancy(int num) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setInCount(event.getInCount() + num);  
            event.setOccupancy(event.getInCount() - event.getOutCount());  
  
            recycleFn.validateOccupancy(event);  
            recycleFn.autoUpdateStatus(event);  
            eventRepository.save(event);  
            template.convertAndSend("/count/occupancy", event);  
            log.info("재실 인원 값 [증가] 성공 - 증가한 수치 : {}, 반영된 현재 방안 인원 수치 : {}", num, event.getOccupancy());  
        } catch (Exception e) {  
            log.error("재실 인원 수 조정 실패 [증가]", e);  
        }  
    }  
  
    // 재실 인원 값 감소 함수  
    public void decreaseOccupancy(int num) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
  
            event.setInCount(event.getInCount() - num);  
            event.setOccupancy(event.getInCount() - event.getOutCount());  
  
            recycleFn.validateOccupancy(event);  
            recycleFn.autoUpdateStatus(event);  
            eventRepository.save(event);  
            template.convertAndSend("/count/occupancy", event);  
            log.info("재실 인원 값 [감소] 성공 - 감소한 수치 : {}, 반영된 현재 방안 인원 수치 : {}", num, event.getOccupancy());  
        } catch (Exception e) {  
            log.error("재실 인원 수 조정 실패 [감소]", e);  
        }  
    }  
  
    // 운영 시작 시간 변경  
    public void setOpenTime(String time) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setOpenTime(time);  
            eventRepository.save(event);  
            template.convertAndSend("/count/time", event);  
            log.info("운영 시작 시간 변경 완료");  
        } catch (Exception e) {  
            log.error("Event 영업시간 로드 실패", e);  
        }  
    }  
  
    // 운영 종료 시간 변경  
    public void setCloseTime(String time) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setCloseTime(time);  
            eventRepository.save(event);  
            template.convertAndSend("/count/time", event);  
            log.info("운영 종료 시간 변경 완료");  
        } catch (Exception e) {  
            log.error("Event 영업시간 로드 실패", e);  
        }  
    }  
  
    // 운영 시간 조회  
    @Transactional(readOnly = true)  
    public void getOperationTime() {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            template.convertAndSend("/count/time", event);  
        } catch (Exception e) {  
            log.error("Event 영업시간 로드 실패", e);  
        }  
    }  
  
    // RelayURL 변경  
    public void setRelayUrl(String url) {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            event.setRelayUrl(url);  
            eventRepository.save(event);  
            template.convertAndSend("/count/relay", event);  
        } catch (Exception e) {  
            log.error("Event Relay URL 변경 실패 - Event ID : {}", event.getId());  
        }  
    }  
  
    // 데이터 로드용  
    public void getInitData() {  
        Event event = null;  
  
        try {  
            event = recycleFn.getEntity(recycleFn.getEntityCount());  
            recycleFn.autoUpdateStatus(event);  
            recycleFn.validateOperationTime(event);  
            eventRepository.save(event);  
            log.info("Event 객체 데이터 로드 완료 - Event ID : {}", event.getId());  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            log.error("Event 객체 데이터 로드 실패", e);  
        }  
    }  
}
```