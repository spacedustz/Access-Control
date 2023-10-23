## 📘 **구현 내용 정리**
- [1. Yaml Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/1-Yaml.md)
- [2. Rabbit Config Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/2-Rabbit.md)
- [3. WebSocket & Web Config](https://github.com/spacedustz/Access-Control/blob/main/Description/3-WebSocket.md)
- [4. 데이터를 담을 DTO](https://github.com/spacedustz/Access-Control/blob/main/Description/4-DTO.md)
- [5. Entity 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/5-Entity.md)
- [6. 초기 데이터 생성, 주기적인 Schedule Task 작업](https://github.com/spacedustz/Access-Control/blob/main/Description/6-ScheduleTask.md)
- [7. MQTT -> Rabbit -> Spring으로 넘어온 데이터 필터링](https://github.com/spacedustz/Access-Control/blob/main/Description/7-Listener.md)
- [8. Event Service 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/8-Service.md)
- [9. Event Controller 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/9-Controller.md)
- [10. RecycleFn 작성 (공통 로직)](https://github.com/spacedustz/Access-Control/blob/main/Description/10-Recycle.md)
- [11. Instance Health Check Thread](https://github.com/spacedustz/Access-Control/blob/main/Description/11-Thread.md)
- [12. 현황판용 페이지 만들기 - 데이터 동기화](https://github.com/spacedustz/Access-Control/blob/main/Description/12-DefaultPage.md)
- [13. 어드민용 관리자 페이지 만들기 - 데이터 동기화](https://github.com/spacedustz/Access-Control/blob/main/Description/13-AdminPage.md)
- [14. Style.css 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/14-Style.md)
- [15. Window Batch 파일 작성 - Server Health Check](https://github.com/spacedustz/Access-Control/blob/main/Description/15-Batch.md)
- [기능 테스트 & 결과물](https://github.com/spacedustz/Access-Control/blob/main/Description/16-Result.md)

---

## 📘 기술 스택
- Spring Data JPA
- Spring AMQP
- Spring Rest Template
- Spring WebFlux (WebClient)
- ~~MariaDB~~  -> 삭제, 오버스펙
- H2 (Imbedded Mode)
- Web Socket
- MQTT
- RabbitMQ
- ~~Viewer (React + TypeScript)~~ -> 삭제, 오버스펙
- Viewer (Vanilla JS + Stomp.js)

---

## 📘 **예상 구현 흐름**

1. 특정 공간에 실시간 카메라가 존재하며, 사람 출입 시 카메라 내부 AI 엔진에서 TripWire Crossing MQTT 이벤트 데이터가 발생합니다.

2. RabbitMQ를 설치하고 Exchange,Queue를 만든 후, Default Exchange -> Custom Exchange -> Routing Key -> Quorum Queue로 바인딩하고 MQTT 데이터를 큐에 쌓습니다.

3. 이때 QuorumQueue의 `x-message-ttl` 등 옵션 파라미터는 요구사항에 맞게 큐의 설정을 미리 해줍니다.

4. RabbitMQ에 쌓인 MQTT 데이터를 Spring에서 RabbitMQ Config를 이용해 Subscribe 해줍니다.

5. Spring에서 받은 데이터는 RabbitMQ Config에 Bean으로 등록한 Converter로 내부적으로 파싱됩니다.

6. RabbitMQ로부터 메시지를 가져올때마다 파싱된 데이터를 Json 계층 구조에 맞게 DTO에 담고 `RabbitTopicListener` 클래스에 나와있는 검증 로직들을 거쳐 엔티티화 -> DB 저장합니다.

7. DB로 저장된 값을 웹소켓 채널을 여러개 열어 목적에 맞는 채널에 데이터를 흘려줍니다.

8. 프론트단에서 Spring의 웹 소켓에 접속해 알맞는 채널에서 데이터들을 받아 변환 후 화면에 출력합니다.

9. Window Batch 파일을 작성해, 서버들 & 인스턴스의 Health Check를 해서 자동으로 재 시작 해줍니다.

<br>

## 📘 **Rabbit MQ**
- RabbitMQ Server를 세팅하고, RabbitMQ Exchange, Queue(Quorum Queue) 생성합니다.
- Default Exchange -> 새로 만든 Exchange로 데이터 라우팅합니다.
- 새로 만든 Exchange -> Quorum Queue에 Routing Key(Topic)를 이용해 바인딩합니다.
- `Topic이란 MQTT 데이터를 내보내는 쪽에서 Topic으로 설정한 key 값입니다.`
- 영상에서 나온 MQTT 이벤트 데이터를 RabbitMQ의 Exchange를 거쳐 알맞는 Routing Key를 가진 Quorum Queue로 데이터가 쌓입니다.

---

## 📘 **Spring**
- Spring에서 RabbitConfig, RabbitTopicListener를 통해 RabbitMQ와 연결, Quorum Queue에 쌓인 데이터를 받아옵니다.
- 받아온 데이터는 RabbitConfig에서 Bean으로 등록한 내부적인 Spring AMQP MessageConverter에 의해 자동으로 변환합니다.
- 변환된 데이터에서 필요한 필드(count 등)을 뽑아 DTO에 넣어 놓습니다.

<br>

> 📌 **DTO -> 엔티티로 변환하기 전 데이터 요구사항**

- 출입 시간 <-> 현재 시간 비교해서 영업 시간이 아닌 경우 Event Trigger 중지, Door 오픈 X
- 운영 시간이 아닐때 Batch 작업 중지, 자동문 API에 문열림 Request 요청 X
- "운영 시간이 아닐 시" DB내 현재 Count Reset
- 현재 방 내부 인원에 따라 Status값 변경 (ex: 15명 이상일때 `만실입니다.` 로 값 변경 후 표시)
- MQTT Tripwire 데이터 중 `crossing_direction`이라는 값이 있습니다.
- 이 값에는 `Up, Down` 2가지가 있는데 방안의 영역에 선을 그어놓고 입장하면 Direction의 값이 In, 나가면 Direction의 값이 Out이 됩니다.
- 이 Up/Down의 수를 엔티티의 변수인 inCount, OutCount로 내부 집계 후 방안의 인원을 Occupancy(현재 방 인원) 값으로 계산 후 출력합니다.

<br>

**위의 과정을 거친 후 WebSocketConfig, HttpHandshakeInterceptor를 작성해 웹소켓 세션을 열어줍니다.**
- Web Socket 세션을 열고 나서 엔티티를 DB에 저장하고 저장한 엔티티를 WebSocket으로 내보냅니다.
- 그 외 앞단에서 필요한 함수들을 그떄그떄 다른 Service (EventService)에서 만들고 Rest API를 만듭니다.
- API 요청/데이터 변경 시, 변경된 엔티티를 웹소켓으로 전달합니다. (화면에 실시간으로 데이터의 수치 변화를 보여주기 위한 용도)

<br>

> 📌 **Schedule Task**

Spring Batch를 쓰려 했으나 너무 오버스펙인 것 같아 Spring 내부 기능인 Schedule 기능을 사용하였습니다.

매일 00시 00분 01초에 Scehdule 기능을 이용하여 매일 00시 00분에, 테이블에 현재 날짜 값을 가진 데이터가 없으면,
자동으로 현재 날짜의 데이터를 생성하게 하는 클래스입니다.

<br>

**run() 함수**
- ApplicationRunner 인터페이스를 이용해 **Spring 어플리케이션 시작 시** 실행됩니다.
- DB에 데이터가 하나도 없으면 초기 데이터 컬럼을 생성합니다.
- 만약 데이터가 1개 이상이라면, addData()함수를 호출해 날짜가 중복되지 않은 새로운 엔티티를 생성합니다.

<br>

**addData() 함수**
- DB에 객체가 1개 이상이고, 데이터의 날짜가 오늘 날짜가 아닐때 오늘 날짜에 해당하는 객체를 새로 생성합니다.

<br>

**healthCheck() 함수**
- 5초 마다 운영시간인지 확인 후, 운영시간이 아니면 객체의 Status 상태를 변화 후 소켓에 전송

> 📌 **View**

- **(완료)** 화면은 단 2개가 필요하므로, React 프로젝트를 제거하고 타임리프를 쓸 필요도 없이 기본 Spring Web에서 진행합니다.
- **(완료)** 데이터를 받아 현재 방안의 인원을 State로 만들어 WebSocket으로부터 데이터를 실시간으로 받아 인원수를 카운팅/표시 합니다.

<br>

자세한 내용은 아래 링크로
- [WebSocket에 접속해 실시간으로 데이터의 변화를 표시할 간단한 Spring Web View](https://github.com/spacedustz/Access-Control/blob/main/Description/View.md)
- [현황판용 화면, 관리자용 값 수정 화면 분리](https://github.com/spacedustz/Access-Control/blob/main/Description/SeparateView.md)

<br>

> 📌 **현황판용 UI에 표시해야 할 데이터**

다른 페이지에서 수치가 바뀌어도 웹소켓으로 인해 이 페이지에서도 변경사항 감지 시 데이터가 동기화 되어 출력됩니다.

- **(완료)** 현재 방안의 인원
- **(완료)** 최대 수용 가능 인원
- **(완료)** 현재 방의 상태

<br>

> 📌 **관리자용 UI에 표시해야 할 데이터 & 기능**

- **(완료)** 운영시간 출력
- **(완료)** 운영시간 변경 기능
- **(완료)** 상태 메시지 변경 기능 - Status가 아닌 CustomStatus를 통해 출력
- **(완료)** 최대 인원 변경 기능 - 현재 방안의 인원, 최대 인원 수 표시
- **(완료)** Door API인 Relay URL 표시
- **(완료)** 운영시간이 아닐 때 현재 방안의 인원 수 0으로 초기화 - ScheduleTask로 인해 1시간마다 주기적 실행
- **(완료)** 현재 재실 인원이 마이너스 값이 나오거나 비정상 수치가 나올때 In/Out Count 초기화 로직 작성
- **(완료)** 현재 재실 인원 변경 기능 추가 - 현재 인원 변경에 따른 In/Out Count 계산 로직 수정

---

## 📘 시퀀스 다이어그램
![시퀀스 다이어그램](https://github.com/spacedustz/Access-Control/blob/main/Description/Diagram.png)

---

## 📘 결과물

아래 링크에 요약해 놓았습니다.

[기능 테스트 & 결과물](https://github.com/spacedustz/Access-Control/blob/main/Description/Result.md)

> 📌 **끝**

- 왼쪽이 현황판용`(http://localhost:8090)`, 오른쪽이 어드민용 페이지`(http://localhost:8090/admin)` 입니다.

처음부터 끝까지 뭘 만들어 보는게 처음이라 많이 헤맸지만 좋은 경험이 된 것 같습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-finish.png)