> 📘 **index.html & index.js**

**index.html**

- Spring Boot 내부 resource/static 디렉터리 내부에 index.html을 만들어 주었습니다.

```html
<!DOCTYPE html>  
<html>  
<head>  
    <meta charset="UTF-8">  
    <title>입장 인원 카운트</title>  
  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.4/sockjs.min.js"></script>  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>  
  
    <script src="index.js"></script>  
  
</head>  
<body>  
  
<div align="center">  
    <div>  
        <h1>현재 : <span id="status"></span></h1>  
    </div>  
  
    <div>  
        <h2>입장 가능 인원: <span id="count" class="text-center"></span>/<span id="max" class="text-center"></span></h2>  
    </div>  
  
    <div>  
        <input type="text" id="new-status" name="newStatus" placeholder="새로운 상태 입력">  
        <button type="button" onclick="updateStatus()">상태 변경</button>  
    </div>  
  
    <div>  
        <input type="text" id="new-max" name="newStatus" placeholder="변경할 최대 인원 수 입력">  
        <button type="button" onclick="updateMaxCount()">최대 인원 변경</button>  
    </div>  
</div>  
  
</body>  
</html>
```

<br>

**index.js**

- Spring WebSocket config에 작성 해놓은 소켓 URL인 `ws`를 `ws://localhost:8090` 뒤에 붙여 `ws://localhost:8090/ws`로 웹 소켓에 연결해줍니다.
- 그리고, Spring에서 RabbitTopicListener 로직 맨 밑에 있던 convertAndSend() 함수에 써놨던 Subscribe URL을 stompClient.subscribe에 넣어줍니다. (`/count/data`)
- 그럼 Event 객체가 브라우저로 넘어옵니다.
- 이 넘어온 Event 객체를 자유롭게 HTML의 body 부분에 쓸 필드를 지정해 사용해서 화면에 출력합니다.

```js
const wsUrl = 'ws://localhost:8090/ws';  
const httpUrl = 'http://localhost:8090/ws';  
  
let socket = new WebSocket(wsUrl);  
let stompClient = Stomp.over(socket);  
  
let roomInfo = {  
    id: null, // ID  
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount    maxCount: 0, // 최대 수용 인원  
    status: "" // Room 상태  
}  
  
stompClient.connect({}, (frame) => {  
    console.log('Connected: ' + frame);  
  
    stompClient.subscribe('/count/data', function (data) {  
        let updatedRoomInfo = JSON.parse(data.body);  
        updateRoomInfo(updatedRoomInfo);  
    });  
});  
  
  
// 렌더링 시 Entity 값 화면에 출력  
window.onload = function () { loadInitialData(); };  
  
function loadInitialData() {  
    fetchJson(httpUrl + '/init')  
        .then(initialRoomInfo => {  
            roomInfo.id = initialRoomInfo.id;  
            roomInfo.occupancy = initialRoomInfo.occupancy;  
            roomInfo.maxCount = initialRoomInfo.maxCount;  
            roomInfo.status = initialRoomInfo.status;  
  
            console.log("초기 정보 로드");  
  
            displayStatus(roomInfo.status);  
            displayOccupancy(roomInfo.occupancy);  
            displayMaxCount(roomInfo.maxCount);  
        });  
}  
  
// Status 값 변경  
function updateStatus() {  
    let newStatusValue= document.getElementById('new-status').value;  
  
    fetchText(httpUrl + '/update-status?status=' + newStatusValue, 'PATCH', {})  
        .then(updatedStatus => {  
            console.log('업데이트 된 상태 : ', updatedStatus);  
            displayStatus(updatedStatus);  
        })  
  
    return false; // 기본 양식 제출 방지  
}  
  
// 방 최대인원 수 (Max Count) 값 변경  
function updateMaxCount() {  
    let newMaxCountValue = document.getElementById('new-max').value;  
  
    fetchJson(httpUrl + '/update-max?max=' + newMaxCountValue, 'PATCH', {})  
        .then(updatedEvent => {  
            console.log('업데이트 된 최대 인원 : ', updatedEvent.maxCount);  
            displayMaxCount(updatedEvent.maxCount);  
        })  
  
    return false; // 기본 양식 제출 방지  
}  
  
// 현재 인원 업데이트 함수  
function updateRoomInfo(updatedData) {  
    displayStatus(updatedData.status);  
    displayOccupancy(updatedData.occupancy);  
    displayMaxCount(updatedData.maxCount);  
}  
  
/* --- Utility 함수 --- */function fetchJson(url, method='GET') {  
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})  
        .then(response => response.json());  
}  
  
function fetchText(url, method='PATCH', body={}) {  
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})  
        .then(response => response.text());  
}  
  
// 방안의 현재 인원  
function displayOccupancy(occupancy) {  
    document.getElementById('count').innerText= occupancy;  
}  
  
// 방안의 상태  
function displayStatus(status) {  
    document.getElementById('status').innerText = status ;  
}  
  
// 최대 인원  
function displayMaxCount(max) {  
    document.getElementById('max').innerText = max;  
}
```

<br>

화면을 보면 Spring의 소켓에 접속해 Event 객체를 받아 객체의 값을 잘 가져오고,

상태값을 바꾸면 객체의 상태값을 DB에서 바꿔서 fetch해서 다시 들고 와서 상태도 잘 업데이트 됩니다.

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-sock.png)

<br>

상태, 최대 입장 가능 인원 변경 기능 (Spring Rest API로 요청을 보내 DB 값을 업데이트 하고 받아옴)

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-done.png)