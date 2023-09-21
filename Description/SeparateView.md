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
  
    <script src="index.js"></script>  
  
</head>  
<body style="background-color: lightblue">  
  
<div align="center">  
    <div>  
        <h1>현재 : <span id="status"></span></h1>  
    </div>  
  
    <div>  
        <h2>입장 가능 인원: <span id="count" class="text-center"></span>/<span id="max" class="text-center"></span></h2>  
    </div>  
</div>  
  
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
  
    <script src="admin.js"></script>  
  
</head>  
<body style="background-color: lightblue">  
  
<div align="center">  
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

```js
const wsUrl = 'ws://localhost:8090/ws';  
const httpUrl = 'http://localhost:8090/ws';  
  
let socket = new WebSocket(wsUrl);  
let stompClient = Stomp.over(socket);  
  
stompClient.connect({}, (frame) => {  
    console.log('Socket Connected : ' + frame);  
    stompClient.subscribe('/count/data', function (data) {  
    });  
});  
  
// Status 값 변경  
function updateStatus() {  
    let newStatusValue= document.getElementById('new-status').value;  
  
    fetchText(httpUrl + '/update-status?status=' + newStatusValue, 'PATCH', {})  
        .then(data => {  
            console.log('업데이트 된 상태 : ', data.status);  
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
  
/* --- Utility 함수 --- */function fetchJson(url, method='GET') {  
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})  
        .then(response => response.json());  
}  
  
function fetchText(url, method='PATCH', body={}) {  
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})  
        .then(response => response.text());  
}
```