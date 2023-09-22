# Access-Control
특정 공간을 카메라로 감시하며, 최대 인원 제한을 설정하고 사람이 들어가고 나갈때 자동문을 열어줍니다.

사람이 들어갈때마다 카운트 수를 실시간으로 증가시키고 화면에 최대 인원에 도달 했을때와 도달 하지 않았을때 UI, 인원 수 수정 로직을 작성합니다.

<br>

## 📘 **구현 내용 정리**
- [Yaml Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/Yaml.md)
- [Rabbit Config Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/Rabbit.md)
- [데이터를 담을 DTO, Entity 설계](https://github.com/spacedustz/Access-Control/blob/main/Description/Entity.md)
- [초기 데이터 생성, 주기적인 Schedule Task 작업](https://github.com/spacedustz/Access-Control/blob/main/Description/Init-Data.md)
- [MQTT -> Rabbit -> Spring으로 넘어온 데이터 필터링](https://github.com/spacedustz/Access-Control/blob/main/Description/Listener.md)
- [WebSocket & HttpHandshakeInterceptor](https://github.com/spacedustz/Access-Control/blob/main/Description/WebSocket.md)
- [브라우저 <-> Spring 간단한 Rest API 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/API.md)
- [WebSocket에 접속해 실시간으로 데이터의 변화를 표시할 간단한 Spring Web View](https://github.com/spacedustz/Access-Control/blob/main/Description/View.md)
- [현황판용 화면, 관리자용 값 수정 화면 분리](https://github.com/spacedustz/Access-Control/blob/main/Description/SeparateView.md)

---

## 📘 **예상 구현 흐름**

특정 공간에 실시간 카메라가 존재하며, 사람 출입 시 카메라 내부 AI 엔진에서 TripWire Crossing MQTT 이벤트 데이터 발생

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

> 📌 **View**

- 화면은 단 2개가 필요하므로, React 프로젝트를 제거하고 타임리프를 쓸 필요도 없이 기본 Spring Web에서 진행합니다.
- 데이터를 받아 현재 방안의 인원을 State로 만들어 WebSocket으로부터 데이터를 실시간으로 받아 인원수를 카운팅/표시 합니다.

<br>

자세한 내용은 아래 링크로
- [WebSocket에 접속해 실시간으로 데이터의 변화를 표시할 간단한 Spring Web View](https://github.com/spacedustz/Access-Control/blob/main/Description/View.md)
- [현황판용 화면, 관리자용 값 수정 화면 분리](https://github.com/spacedustz/Access-Control/blob/main/Description/SeparateView.md)

<br>

> 📌 **현황판용 UI에 표시해야 할 데이터**

- 현재 방안의 인원
- 최대 수용 가능 인원
- 현재 방의 상태

<br>

> 📌 **관리자용 UI에 표시해야 할 데이터 & 기능**

- 운영시간
- 상태 메시지 변경 기능
- 최대 인원 변경 기능 (현재 방안의 인원, 최대 인원 수 표시)
- Door API인 Relay URL 표시
---

## 📘 기술 스택
- Spring Web (방 내부의 인원수, 최대 수용 인원, 현재 방 상태 등 UI 출력)
- Spring Data JPA
- Lombok
- WebSocket (STOMP)
- RabbitMQ (AMQP)
- H2 (Embedded Mode 사용)

---

## 📘 시퀀스 다이어그램
![시퀀스 다이어그램](https://github.com/spacedustz/Access-Control/blob/main/Description/Diagram.png)

---

## 📘 결과물

테스트 API를 만들어 Occupancy의 값을 조정하여 특정 수치마다 글씨 색, 상태 값을 변경하는 API 요청을 소켓으로 보내 데이터를 변경

변경한 데이터를 소켓에서 다시 받아와서 변경된 엔티티의 Occupancy, MaxCount, Status 값 등 실시간 업데이트

<br>

**방안의 사람수가 10명 이하 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-1.png)

<br>

**방안의 사람수가 10명 이상 15명 이하 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-2.png)
<br>

**방안의 사람수가 15명 이상 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-3.png)