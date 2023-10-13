## 📘 기능 테스트 & 결과물

> 📌 **Admin 페이지**

기능 테스트는 제가 만든 Admin 페이지의 값들을 변경하는 기능을 이용해 진행하였습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-admin.png)

<br>

> 📌 **운영 시간 변경 기능**

운영 시간을 바꾸면, 서버 내부적으로 현재 시간과 비교해서 운영 시간이 아니면,

상태값 자동 변경 및, 빨간 체크 표시 이미지로 자동 변경되며, 소켓을 통해 실시간으로 반영됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-operation-time.png)

5초마다 현재 시간과 객체의 운영 시작 & 종료 시간을 Health Check하는 함수가 추가로 실행되며,

운영 시간이 아니면, 상태를 **운영 시간이 아닙니다.** 로 변경합니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-close.png)

<br>

> 📌 **입장 가능 상태**

방안의 사람수가 10명 이하 일때 현재 방 상태 값, 색생 자동 변경

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-1.png)

<br>

> 📌 **혼잡 상태**

방안의 사람수가 10명 이상이고, 최대 인원 이하 일때 현재 방 상태 값, 색생 자동 변경

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-2.png)
<br>

> 📌 **입장 불가 상태**

방안의 사람수가 최대 인원 이상 일때 현재 방 상태 값, 색생 자동 변경

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-3.png)

<br>

>📌 **Custom 상태 메시지 설정**

방이 내부적으로 수리중이거나, 입장할 수 없는 상태일 경우, 기본 Status가 아닌 Custom Status값을 이용해 상태를 변경 시킵니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-custom-status.png)

<br>

> 📌 **현재 인원 변경 기능**

현재 방안의 인원을 변경하는 기능입니다.

이 기능이 왜 필요하냐면, 카메라 내부의 AI 엔진이 100% 정확하지 않고,

사람이 동시에 5~6명이 입장하면 잘못된 카운팅을 할 수 있기 때문에 변경 기능을 넣었습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy.png)

<br>

우선 5명을 증가 시켜 보겠습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy3.png)

서버 내부적으로 Incount, OutCount를 이용해 현재 방안의 인원을 5로 조정하고 데이터베이스에 저장합니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy2.png)

그 후, 5초마다 서버에서 HealCheck 함수가 돌거나, 웹소켓으로 인해 현황판에도 값이 실시간으로 바로 올라옵니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy4.png)

<br>

감소도 똑같이 5를 낯추면 동일하게 동작하는 걸 볼 수 있습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy6.png)

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy5.png)

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-occupancy7.png)




<br>

> 📌 **최대 입장 인원 변경 기능**

최대 입장 인원을 변경하는 기능입니다.

**입장이 불가합니다.** 상태를 출력하는 기준은 항상 재실 인원이 최대 인원에 도달했을때 입니다.

기본값은 15로 설정해뒀지만 임의로 20으로 바꿔 보겠습니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-max-count-desc.png)

<br>

이제 이 상태에서 기존 최대 인원이 15일때 **입장이 불가합니다.** 로 뜨던 상태가, 20이 되면 뜨게 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-max-count3.png)

<br>

당연히 **혼잡 상태**도 최대 인원이 아니며, 10명 이상일때 **혼잡 상태**로 뜨게 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-max-count2.png)

<br>

> 📌 **Relay URL 변경 기능**

단순히 문에 API 요청을 보낼때 사용하는 URL을 데이터베이스의 객체에 저장하며,

RabbitMQ에서 이벤트 메시지가 올때마다 Spring Rest Template을 사용해 GET 요청을 보내서 문을 열어주게 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-relay.png)