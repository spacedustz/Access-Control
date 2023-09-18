# Access-Control
특정 공간을 카메라로 감시하며, 최대 인원 제한을 설정하고 사람이 들어가고 나갈때 자동문을 열어줍니다.
사람이 들어갈때마다 카운트 수를 실시간으로 증가시키고 화면에 최대 인원에 도달 했을때와 도달 하지 않았을때 UI, 인원 수 수정 로직을 작성합니다.

<br>

**UI에 표시해야 할 데이터**
- 현재 방안의 인원
- 최대 수용 가능 인원

<br>

**예상 구현 흐름**
1. 특정 공간에 실시간 카메라 존재
2. 사람 출입 시 딥러닝 엔진에서 이벤트 발생 (TripWire Crossing)
3. 영상에서 나온 MQTT 이벤트 데이터를 RabbitMQ의 Exchange를 거쳐 맞는 Routing Key를 가진 Quorum Queue로 데이터 쌓기
4. Quorum Queue에 쌓인 데이터를 Spring에서 가져와 필요한 필드(count 등)을 뽑아 엔티티화 -> DB 저장
5. Restful API & WebSocket을 통해 프론트엔드로 데이터 전달
6. 프론트엔드에서 데이터를 받아 현재 방안의 인원을 State로 만들어 실시간으로 인원수를 카운팅 합니다.

<br>

**구현 조건**
- 출입 시간 <-> 현재 시간 비교해서 영업 시간이 아닌 경우 Event Trigger 중지, Door 오픈 X
- 운영 시간이 아닐때 Batch 작업 중지, UI에 영업중단 표시, 자동문 API에 문열림 방지 Request 보내기
- "운영 시간이 아닐 시" DB내 현재 Count Reset
- 15명 이상일때 `만실입니다.` 로 표시- 재실 인원 보정 변수 고장 남
- 운영 시간이 아닐 때 `현재는 운영 시간이 아닙니다.` 로 표시
- Tripwire Direction In/Out별 Count 수 집계


---

## 기술 스택
- Spring Batch
- Spring Web
- Spring Data JPA
- Lombok
- WebSocket (STOMP)
- RabbitMQ (AMQP)
- H2

---

## 시퀀스 다이어그램
![시퀀스 다이어그램](https://github.com/spacedustz/Access-Control/blob/main/Description/Diagram.png)