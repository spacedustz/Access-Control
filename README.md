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
5. Restful API & WebSocket을 통해 Spring Web에서 js 전달
6. 프론트엔드에서 데이터를 받아 현재 방안의 인원을 State로 만들어 실시간으로 인원수를 카운팅 합니다.

- 출입 시간 <-> 현재 시간 비교해서 영업 시간이 아닌 경우 Event Trigger 중지, Door 오픈 X
- 운영 시간이 아닐때 Batch 작업 중지, UI에 영업중단 표시, 자동문 API에 문열림 방지 Request 보내기
- "운영 시간이 아닐 시" DB내 현재 Count Reset
- 15명 이상일때 `만실입니다.` 로 표시- 재실 인원 보정 변수 고장 남
- 운영 시간이 아닐 때 `현재는 운영 시간이 아닙니다.` 로 표시
- Tripwire Direction In/Out별 Count 수 내부 집계 후 Occupancy(현재 방 인원) 값 계산


---

## 기술 스택
- Spring Web (방 내부의 인원수, 최대 수용 인원, 현재 방 상태 등 UI 출력)
- Spring Data JPA
- Lombok
- WebSocket (STOMP)
- RabbitMQ (AMQP)
- H2 (Embedded Mode 사용)

---

## 시퀀스 다이어그램
![시퀀스 다이어그램](https://github.com/spacedustz/Access-Control/blob/main/Description/Diagram.png)

---

## RabbitMQ Receiver

[My Github Repository](https://github.com/spacedustz/Access-Control)

<br>

**흐름**
- RabbitMQ에서 MQTT 데이터를 Queue에 쌓습니다.
- Spring Boot에서 Queue에 쌓인 메시지를 가져옵니다.
- 가져올때 해당 데이터의 구조에 맟춰서 DTO를 작성해줍니다.
- 메시지를 받을 Receiver를 작성할 때 파라미터로 넣어주면, 내부적으로 Bean으로 주입한 MessageConverter가 데이터를 변환해서 DTO에 담아줍니다.
- DTO에 담긴 데이터를 엔티티화 해서 DB에 저장합니다.
- MQTT의 필드 중 출입 Direction 필드를 빼서 출/입 변수를 만들어 엔티티화해서 저장해줍니다.
- DB에 저장한 데이터를 브라우저와 소켓 통신을 해서 실시간으로 값의 변화를 출력합니다.

---
## 구현

- [Yaml Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/Yaml.md)
- [Rabbit Config Setting](https://github.com/spacedustz/Access-Control/blob/main/Description/Rabbit.md)
- [데이터를 담을 DTO, Entity 설계](https://github.com/spacedustz/Access-Control/blob/main/Description/Entity.md)
- [초기 데이터 생성, 주기적인 Schedule Task 작업](https://github.com/spacedustz/Access-Control/blob/main/Description/Init-Data.md)
- [MQTT -> Rabbit -> Spring으로 넘어온 데이터 필터링](https://github.com/spacedustz/Access-Control/blob/main/Description/Listener.md)
- [WebSocket & HttpHandshakeInterceptor](https://github.com/spacedustz/Access-Control/blob/main/Description/WebSocket.md)
- [브라우저 <-> Spring 간단한 Rest API 작성](https://github.com/spacedustz/Access-Control/blob/main/Description/API.md)
- [WebSocket에 접속해 실시간으로 데이터의 변화를 표시할 간단한 Spring Web View](https://github.com/spacedustz/Access-Control/blob/main/Description/View.md)
- [전광판용 화면, 관리자용 값 수정 화면 분리](https://github.com/spacedustz/Access-Control/blob/main/Description/SeparateView.md)

<br>

## 결과물

디자인 수정 중, 수정된 값들은 웹 소켓을 통해 페이지 새로고침이나 Re-Rendering 없이 실시간으로 기본 페이지의 값, 어드민 페이지의 값이 업데이트 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-done2.png)

<br>

**방안의 사람수가 10명 이하 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-1.png)

<br>

**방안의 사람수가 10명 이상 15명 이하 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-2.png)
<br>

**방안의 사람수가 15명 이상 일때 현재 방 상태 값, 색생 자동 변경 (스타일은 여전히 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-3.png)