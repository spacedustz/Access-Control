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

<br>

**최종 결과물 (스타일은 여전히 수정중)**

- 왼쪽이 현황판용`(http://localhost:8090)`, 오른쪽이 어드민용 페이지`(http://localhost:8090/admin)` 입니다.
- 오른쪽 창의 개발자 도구를 보면 웹 소켓 채널이 6개가 열려있고, 내부 로직을 작성할때 적절한 채널로 들어와서 화면을 실시간으로 업데이트 합니다.
- 데이터가 업데이트 되면, 현황판용 & 어드민용 페이지에 둘다 실시간으로 변경된 값이 반영이 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-final.png)