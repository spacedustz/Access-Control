## 📘 **전광판용 & 관리자용 페이지 분리 - WebSocket 화면 데이터 동기화**

전광판에 표시될 현재 입장 가능인원, 관리자용 최대인원 & 상태 수정 페이지를 따로 나눴습니다.

<br>

**전광판용** : `index.html, index.js`

- 전광판용에는 현재 xx실 출입 가능인원, 현재 인원이 출력됩니다.
- Spring Socket에서 값을 불러와 엔티티가 변할때마다 & 새로운 트리거 이벤트가 넘어올때마다 수치를 화면에 반영합니다.
- 관리자 페이지에선 최대인원, 현재 상태를 바꿀수 있는데 그 바꾼 수치의 화면 동기화를 위해, Status와 maxCount를 수정하는 Spring Service 함수 내부에서도 웹소켓으로 데이터를 전달해 HTML Element에 바로 반영되게 적용했습니다.

```html
<!DOCTYPE html>  
<html>  
<head>  
    <meta charset="UTF-8">  
    <title>입장 인원 카운트</title>  
  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.4/sockjs.min.js"></script>  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>  
    <link rel="stylesheet" href="style.css">  
</head>  
  
<body style="background-color: lightblue">  
  
<div align="center">  
    <div>  
        <h1><span id="status"></span></h1>  
    </div>  
  
    <br>  
  
    <div>  
        <h2>입장 가능 인원: <span id="count" class="text-center"></span>/<span id="max" class="text-center"></span></h2>  
    </div>  
</div>  
  
<script src="index.js"></script>  
</body>  
</html>
```

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
  
// 현재 방 인원에 따라 Status 글씨 색상 바꾸기  
function changeStatusColor(occupancy) {  
    var status = document.getElementById('status');  
  
    if (occupancy < 10) {  
        status.style.color = 'green';  
    } else if (occupancy > 12) {  
        status.style.color = 'orange';  
    } else if (occupancy > 14) {  
        status.style.color = 'red';  
    }  
}  
  
function displayStatusWithColor(status, occupancy) {  
    let coloredStatus = document.getElementById('status').innerText = status;  
  
    if (occupancy < 10) {  
        status.style.color = 'green';  
    } else if (occupancy > 12) {  
        status.style.color = 'orange';  
    } else if (occupancy > 14) {  
        status.style.color = 'red';  
    }  
}  
  
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
            changeStatusColor();  
        });  
}  
  
// 현재 인원 업데이트 함수  
function updateRoomInfo(updatedData) {  
    displayStatus(updatedData.status);  
    displayOccupancy(updatedData.occupancy);  
    displayMaxCount(updatedData.maxCount);  
    changeStatusColor();  
}  
  
// 최대 인원  
function displayMaxCount(max) {  
    document.getElementById('max').innerText = max;  
}  
  
// 방안의 현재 인원  
function displayOccupancy(occupancy) {  
    document.getElementById('count').innerText= occupancy;  
}  
  
// 방안의 상태  
function displayStatus(status) {  
    document.getElementById('status').innerText = status;  
}  
  
/* --- Utility 함수 --- */function fetchJson(url, method='GET') {  
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})  
        .then(response => response.json());  
}
```

<br>

**관리자용** : `admin.html, admin.js`

- Spring 소켓에 접속한 상태
- Spring Rest API에 요청을 보내 최대 입장 가능 인원, 현재 xx실의 상태를 입력하면 Spring JPA Entity의 값을 변경
- 그 값을 소켓을 통해 index.js로 넘겨 전광판용 화면에 실시간으로 적용되게 하였습니다.

```html
<!DOCTYPE html>  
<html>  
<head>  
    <meta charset="UTF-8">  
    <title>입장 인원 카운트</title>  
  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.4/sockjs.min.js"></script>  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>  
    <link rel="stylesheet" href="style.css">  
</head>  
  
<body style="background-color: lightblue">  
  
<div align="center">  
    <h1>운영 시간 : <span id="time"></span></h1>  
</div>  
  
<div align="center">  
    <h2>상태 메시지 변경</h2>  
    <p>현재 상태 메시지 : <span id="status"></span></p>  
    <div>  
        <input type="text" id="new-status" name="newStatus" placeholder="새로운 상태 입력">  
        <button type="button" onclick="updateStatus()">상태 변경</button>  
    </div>  
  
    <div>  
        <input type="text" id="close-room" name="closeRoom" placeholder="고장 or 수리중">  
        <button type="button" onclick="closeRoom()">영업 중지</button>  
    </div>  
</div>  
  
<br>  
  
<div align="center">  
    <h2>최대 인원 변경</h2>  
    <p>현재 재실 인원 : <span id="count"></span></p>  
    <p>최대 인원 : <span id="max"></span></p>  
    <div>  
        <input type="text" id="new-max" name="newStatus" placeholder="변경할 최대 인원 수 입력">  
        <button type="button" onclick="updateMaxCount()">최대 인원 변경</button>  
    </div>  
</div>  
  
<div align="center">  
    <h2>Relay URL</h2>  
    <p><span id="url"></span></p>  
</div>  
  
  
<script src="admin.js"></script>  
</body>  
</html>
```

```js
const wsUrl = 'ws://localhost:8090/ws';  
const httpUrl = 'http://localhost:8090/ws';  
  
let socket = new WebSocket(wsUrl);  
let stompClient = Stomp.over(socket);  
  
let roomInfo = {  
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount    maxCount: 0, // 최대 수용 인원  
    status: "", // Room 상태  
    relayUrl: "" // Relay URL  
};  
  
const time = {  
    value: "09:00 - 18:00"  
};  
  
stompClient.connect({}, (frame) => {  
    console.log('Socket Connected : ' + frame);  
    stompClient.subscribe('/count/data', function (data) {  
        let updatedRoomInfo = JSON.parse(data.body);  
        showStats(updatedRoomInfo);  
    });  
});  
  
window.onload = function () { getData(); };  
  
function showStats(data) {  
    displayStatus(data.status);  
    displayOccupancy(data.occupancy);  
    displayMaxCount(data.maxCount);  
    displayOperationTime();  
    displayRelayUrl(data.relayUrl);  
}  
  
function getData() {  
    fetchJson(httpUrl + '/stat')  
        .then(data => {  
            roomInfo.occupancy = data.occupancy;  
            roomInfo.maxCount = data.maxCount;  
            roomInfo.status = data.status;  
            roomInfo.relayUrl = data.relayUrl;  
  
            displayStatus(roomInfo.status);  
            displayOccupancy(roomInfo.occupancy);  
            displayMaxCount(roomInfo.maxCount);  
            displayOperationTime();  
            displayRelayUrl(roomInfo.relayUrl);  
        });  
}  
  
// Status 값 변경  
function updateStatus() {  
    let newStatusValue= document.getElementById('new-status').value;  
  
    fetchText(httpUrl + '/update-status?status=' + newStatusValue, 'PATCH', {})  
        .then(data => {  
            console.log('업데이트 된 상태 : ', data);  
        })  
  
    return false; // 기본 양식 제출 방지  
}  
  
// 영업 불가일때 상태 변경  
function closeRoom() {  
    let closeRoomValue = document.getElementById('close-room').value;  
  
    fetchText(httpUrl + '/update-status?status=' + closeRoomValue, 'PATCH', {})  
        .then(data => {  
            console.log('업데이트 된 상태 : ', data);  
        })  
  
    return false; // 기본 양식 제출 방지  
}  
  
// 방 최대인원 수 (Max Count) 값 변경  
function updateMaxCount() {  
    let newMaxCountValue = document.getElementById('new-max').value;  
  
    fetchJson(httpUrl + '/update-max?max=' + newMaxCountValue, 'PATCH', {})  
        .then(data => {  
            console.log('최대 인원 업데이트 완료 : ', data.maxCount);  
        })  
  
    return false; // 기본 양식 제출 방지  
}  
  
// 최대 인원  
function displayMaxCount(max) {  
    document.getElementById('max').innerText = max;  
}  
  
// Relay URL  
function displayRelayUrl(url) {  
    document.getElementById('url').innerText = url;  
}  
  
// 운영 시간  
function displayOperationTime() {  
    document.getElementById('time').innerText = time.value;  
}  
  
// 방안의 현재 인원  
function displayOccupancy(occupancy) {  
    document.getElementById('count').innerText= occupancy;  
}  
  
// 방안의 상태  
function displayStatus(status) {  
    document.getElementById('status').innerText = status;  
}  
  
/* --- Utility 함수 --- */function fetchJson(url, method='GET') {  
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})  
        .then(response => response.json());  
}  
  
function fetchText(url, method='PATCH', body={}) {  
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})  
        .then(response => response.text());  
}
```

<br>

**CSS**

스타일은 계속 수정중이지만 지금은 아주 간단하게 해놨습니다.

```css
/* Button */  
button {  
    background-color: #4CAF50;  
    color: white;  
    padding: 10px 20px;  
    border: none;  
    border-radius: 4px;  
    cursor: pointer;  
}  
  
button:hover {  
    background-color: #45a049;  
}  
  
/* Input */  
input[type="text"],  
input[type="number"] {  
    padding: 8px;  
    border-radius: 4px;  
}  
  
/* Div */  
div {  
    margin-bottom:.8rem;  
    padding:.8rem;  
    background-color:#f9f9f9;  
    border-radius:.3rem;  
    box-shadow:.1rem .1rem .3rem rgba(0,0,0,.2);  
}  
  
/* Span */  
span {  
    font-weight:bold  
}  
  
/* Paragraph */  
p {  
    font-size :18px  
}
```

<br>

**결과물 (스타일 수정중)**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-done2.png)