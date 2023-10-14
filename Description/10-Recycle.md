## 📘 RecycleFn

비즈니즈 로직에서 공통으로 쓰이는 함수들을 모아 놓은 클래스입니다.

- @Cacheable getEntity, getEnttyCount : 객체를 조회하고 캐싱하는 함수
- initCount : 단순히 객체의 모든 Count 값을 초기화 하는 함수
- timeFormatter : 년월일 / 시분을 변환해 문자열로 반환하는 함수
- autoUpdateStatus : 현재 인원에 따른 객체 상태 업데이트
- validateOperation Time : 현황판의 운영 상태 검증
- validateOccupancy : 현재 인원의 비정상 카운팅 검증
- validateOperatingStatus : 들어오는 이벤트의 시간이 운영시간에 해당하는지 검증

```java
@Slf4j  
@Service  
@Transactional  
@RequiredArgsConstructor  
public class RecycleFn {  
    private final EventRepository eventRepository;  
    private final SimpMessagingTemplate template;  
    private String currentDate = String.valueOf(LocalDate.now());  
  
    @Cacheable("entityCount")  
    public Long getEntityCount() {  
        return eventRepository.count();  
    }  
  
    @Cacheable("entity")  
    public Event getEntity(Long pk) {  
        return eventRepository.findById(pk).orElseThrow(() -> new CommonException("Data-001", HttpStatus.NOT_FOUND));  
    }  
  
    // 엔티티 수치 초기화 함수  
    public void initiateCount(Event event) {  
        event.setOccupancy(0);  
        event.setInCount(0);  
        event.setOutCount(0);  
    }  
  
    // 년-월-일 변환 함수  
    public String ymdFormatter(@Nullable LocalDateTime dateTime) {  
        DateTimeFormatter YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");  
        return dateTime.format(YMDFormatter);  
    }  
  
    // 시-분 변환 함수  
    public String hmFormatter(@Nullable LocalDateTime date) {  
        DateTimeFormatter HMFormatter = DateTimeFormatter.ofPattern("HH:mm");  
        return date.format(HMFormatter);  
    }  
  
    // 현재 인원에 따른 객체 상태 업데이트  
    public void autoUpdateStatus(Event event) {  
        if (event.getCustomStatus().equals("")) {  
            if (event.getOccupancy() <= 9) {  
                event.setStatus(Status.LOW);  
            } else if (event.getOccupancy() >= 10 && event.getOccupancy() < event.getMaxCount()) {  
                event.setStatus(Status.MEDIUM);  
            } else if (event.getOccupancy() >= event.getMaxCount()) {  
                event.setStatus(Status.HIGH);  
            }  
        } else {  
            log.info("현재 방안의 상태 - {}", event.getCustomStatus());  
        }  
    }  
  
    // 운영 시간 검증 함수  
    public Event validateOperationTime(Event event) {  
        String openTime = event.getOpenTime();  
        String closeTime = event.getCloseTime();  
  
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");  
  
        LocalDateTime nowTime = LocalDateTime.now();  
  
        LocalTime now = LocalTime.parse(hmFormatter(nowTime));  
  
        // openTime과 closeTime을 LocalDateTime으로 변환  
        LocalTime open = LocalTime.parse(openTime, timeFormatter);  
        LocalTime close = LocalTime.parse(closeTime, timeFormatter);  
  
        // 운영 시간 검증  
        if (now.isAfter(open) && now.isBefore(close)) {  
            log.info("정상 운영 시간 입니다. [ 운영 시간 ] {} - {}", open, close);  
            autoUpdateStatus(event);  
        } else {  
            event.setStatus(Status.NOT_OPERATING);  
            initiateCount(event);  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 현재 시간 : {}", openTime, closeTime, now);  
        }  
  
        return event;  
    }  
  
    // 재실 인원 검증 함수  
    public void validateOccupancy(Event event) {  
        try {  
            if (event.getOccupancy() < 0) {  
                initiateCount(event);  
                eventRepository.save(event);  
  
                log.error("재실 인원 오류 - In/Out Count, Occupancy 초기화 - 초기화 된 Occupancy 값 : {}", event.getOccupancy());  
            }  
  
            if (event.getOccupancy() >= event.getMaxCount()) {  
                log.info("인원 초과 - 재실 인원/최대인원 : {}명/{}명", event.getOccupancy(), event.getMaxCount());  
            }  
  
            template.convertAndSend("/count/data", event);  
        } catch (Exception e) {  
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId(), e);  
        }  
    }  
  
    // 운영시간 검증 함수  
    public void validateOperatingStatus(String entityYMDDate,  
                                        String eventYMDDate,  
                                        LocalTime open,  
                                        LocalTime close,  
                                        LocalTime eventDateTime,  
                                        String openTime,  
                                        String closeTime,  
                                        Event event) {  
  
        // 이벤트 데이터의 날짜 검증  
        if (!eventYMDDate.equals(currentDate) || (!entityYMDDate.equals(currentDate))) {  
            log.error("데이터의 날짜가 오늘 날짜가 아닙니다. - 현재 날짜 : {}, 데이터의 날짜 : {}", currentDate, eventYMDDate);  
        }  
  
        // 이벤트 데이터의 운영 시간 검증  
        if (!eventDateTime.isAfter(open) && !eventDateTime.isBefore(close)) {  
            event.setStatus(Status.NOT_OPERATING);  
            log.error("운영 시간이 아닙니다. - 운영 시간 : {} - {}, 입장한 시간 : {}", openTime, closeTime, eventDateTime);  
        }  
  
        try {  
            eventRepository.save(event);  
        } catch (Exception e) {  
            log.error("Occupancy, In/Out Count 값 초기화 후 객체 저장 실패 - Event ID : {}", event.getId());  
        }  
  
        // Web Socket Session에 Event 객체 전달  
        template.convertAndSend("/count/data", event);  
    }  
}
```