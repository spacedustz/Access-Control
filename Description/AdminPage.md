## 📘 **어드민용 관리 페이지 - 데이터 동기화**

백엔드에서 열어준 소켓 채널들을 Subscribe 해서 Admin 페이지의 기능 버튼을 누르면,

백엔드 서버의 void 함수들이 로직 실행 결과가 담긴 엔티티를 각 소켓 채널로 밀어줍니다.

그 소켓에서 받은 데이터들을 HTML 요소들과 적절하게 매핑 & 사용합니다.

- Spring 소켓에 접속한 상태
- Spring Rest API에 요청을 보내 최대 입장 가능 인원, 현재 xx실의 상태를 입력하면 Spring JPA Entity의 값을 변경
- 그 값을 소켓을 통해 index.js로 넘겨 현황판용 화면에 실시간으로 적용되게 하였습니다.

<br>

> 📌 **admin.html**

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
  
<body class="admin-body">  
  
<section class="admin-container1">  
    <div class="admin-div">  
        <h1 class="admin-color">[ 현재 운영 시간 ]</h1>  
        <span id="open" class="admin-color"></span> - <span id="close" class="admin-color"></span>  
  
        <br>  
  
        <h2 class="admin-color">[ 운영 시간 변경 ]</h2>  
        <p style="color: azure; font-weight: bold; font-size: 16px;">※ 시작 시간과 종료 시간을 항상 같이 입력해 주세요. (숫자만)</p>  
        <p style="font-size: 11px;"><strong class="admin-color">시작 시간</strong>  
            <input type="text" id="new-start-open" class="time" name="newOpenTime" placeholder="00~23">  
            <strong class="admin-color">:</strong>  
            <input type="text" id="new-start-close" class="time" name="newCloseTime" placeholder="00~59">  
        </p>  
  
        <p style="font-size: 11px;"><strong class="admin-color">종료 시간</strong>  
            <input type="text" id="new-end-open" class="time" name="newOpenTime" placeholder="00~23">  
            <strong class="admin-color">:</strong>  
            <input type="text" id="new-end-close" class="time" name="newCloseTime" placeholder="00~59">  
        </p>  
  
        <button type="button" onclick="updateOperationTime()">운영시간 변경</button>  
    </div>  
  
    <br>  
  
    <div class="admin-div">  
        <h2 class="admin-color">[ 상태 메시지 변경 ]</h2>  
        <p class="admin-color">현재 상태 메시지 : <span id="status"></span></p>  
  
        <p style="color: azure; font-weight: bold; font-size: 16px;">※ 임의로 설정한 상태 메시지를 초기화 하려면 입력창이 빈칸인 상태에서 상태 변경을 눌러주세요.</p>  
  
        <input type="text" id="new-status" name="newStatus" placeholder="새로운 상태 입력">  
        <button type="button" onclick="updateStatus()">상태 변경</button>  
    </div>  
</section>  
  
<br>  
  
<section class="admin-container2">  
    <div class="admin-div">  
        <h2 class="admin-color">[ 재실 인원 변경 ]</h2>  
        <p class="admin-color">현재 재실 인원 : <span id="count"></span></p>  
  
        <input type="text" id="increase-occupancy" name="IncreaseOccupancy" placeholder="증가 시킬 수 입력">  
        <button type="button" onclick="increaseOccupancy()">재실 인원 증가</button>  
  
        <br>  
  
        <input type="text" id="decrease-occupancy" name="DecreaseOccupancy" placeholder="감소 시킬 수 입력">  
        <button type="button" onclick="decreaseOccupancy()">재실 인원 감소</button>  
    </div>  
  
    <br>  
  
    <div class="admin-div">  
        <h2 class="admin-color">[ 최대 인원 변경 ]</h2>  
        <p class="admin-color">현재 최대 인원 : <span id="max"></span></p>  
  
        <input type="text" id="new-max" name="newStatus" placeholder="변경할 최대 인원 수 입력">  
        <button type="button" onclick="updateMaxCount()">최대 인원 변경</button>  
  
    </div>  
</section>  
  
<br>  
  
<div class="admin-div">  
    <h2 class="admin-color">[ Relay URL 변경 ]</h2>  
    <p class="admin-color">현재 Relay URL : <span id="url" class="admin-color"></span></p>  
  
    <input type="text" id="new-relay" name="newRelay" placeholder="변경할 URL 입력">  
    <button type="button" onclick="updateRelayUrl()">URL 변경</button>  
</div>  
  
<script src="admin.js"></script>  
</body>  
</html>
```

<br>

> 📌 **admin.js**

```js
const wsUrl = 'ws://localhost:8090/ws';  
const httpUrl = 'http://localhost:8090/ws';  
  
let socket = new WebSocket(wsUrl);  
let stompClient = Stomp.over(socket);  
  
let roomInfo = {  
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount    maxCount: 0, // 최대 수용 인원  
    status: "", // Room 상태 (Spring Enum : Status)    customStatus: "", // Custom Status  
    relayUrl: "", // Relay URL  
    openTime: "", // 운영 시작 시간  
    closeTime: "", // 운영 종료 시간  
};  
  
// 소켓 채널  
stompClient.connect({}, (frame) => {  
    console.log('Socket Connected : ' + frame);  
    stompClient.subscribe('/count/data', function (data) {  
        let entity = JSON.parse(data.body);  
        showStats(entity);  
    });  
  
    stompClient.subscribe('/count/status', function (data) {  
        let entity = data.body;  
        roomInfo.status = entity.status  
        displayStatus(roomInfo.status);  
    });  
  
    stompClient.subscribe('/count/time', function (data) {  
        let entity = JSON.parse(data.body);  
        roomInfo.openTime = entity.openTime;  
        roomInfo.closeTime = entity.closeTime  
        displayOperationOpenTime(roomInfo.openTime);  
        displayOperationCloseTime(roomInfo.closeTime);  
  
        console.log('변경된 운영 시간 : ', entity.openTime + ':' + entity.closeTime);  
    });  
  
    stompClient.subscribe('/count/customStatus', function (data) {  
        let entity = JSON.parse(data.body);  
        roomInfo.customStatus = entity.customStatus  
        displayStatus(roomInfo.customStatus);  
  
        console.log('Custom 상태 업데이트 : ', entity.customStatus);  
    });  
  
    stompClient.subscribe('/count/occupancy', function (data) {  
        let entity = JSON.parse(data.body)  
        roomInfo.occupancy = entity.occupancy;  
        displayOccupancy(roomInfo.occupancy);  
  
        console.log('재실 인원 업데이트 : ', entity.occupancy);  
    });  
  
    stompClient.subscribe('/count/relay', function (data) {  
        let entity = JSON.parse(data.body)  
        roomInfo.relayUrl = entity.relayUrl;  
        displayRelayUrl(roomInfo.relayUrl);  
  
        console.log('Relay URL 업데이트 : ', entity.relayUrl);  
    });  
});  
  
// 렌더링 시, 초기 데이터 값 출력  
window.onload = function () {  
    getData();  
};  
  
function getData() {  
    fetchJson(httpUrl + '/init')  
        .then(data => {  
            showStats(data);  
        });  
    return false;  
}  
  
// 데이터 출력  
function showStats(data) {  
    roomInfo.maxCount = data.maxCount;  
    roomInfo.customStatus = data.customStatus;  
    roomInfo.relayUrl = data.relayUrl;  
    roomInfo.openTime = data.openTime;  
    roomInfo.closeTime = data.closeTime;  
    roomInfo.status = data.status;  
  
    if (data.occupancy < 0) {  
        let initOccupancy = 0;  
        displayOccupancy(initOccupancy);  
    } else {  
        roomInfo.occupancy = data.occupancy;  
        displayOccupancy(roomInfo.occupancy);  
    }  
  
    switch (data.status) {  
        case "LOW":  
            roomInfo.status = "입장 가능합니다.";  
            displayStatus(roomInfo.status);  
            break;  
        case "MEDIUM":  
            roomInfo.status  = "조금 혼잡합니다.";  
            displayStatus(roomInfo.status);  
            break;  
        case "HIGH":  
            roomInfo.status  = "입장이 불가합니다.";  
            displayStatus(roomInfo.status);  
            break;  
        case "NOT_OPERATING":  
            roomInfo.status  = "운영시간이 아닙니다.";  
            displayStatus(roomInfo.status);  
            break;  
    }  
  
    displayMaxCount(roomInfo.maxCount);  
    displayRelayUrl(roomInfo.relayUrl);  
    displayOperationOpenTime(roomInfo.openTime);  
    displayOperationCloseTime(roomInfo.closeTime);  
    displayStatus(roomInfo.status);  
}  
  
// Status 값 변경  
function updateStatus() {  
    let newStatusValue = document.getElementById('new-status').value;  
    fetchJson(httpUrl + '/update-status?status=' + newStatusValue, 'PATCH', {})  
  
    document.getElementById('new-status').value = null;  
    return false; // 기본 양식 제출 방지  
}  
  
// 방 최대인원 수 (Max Count) 값 변경  
function updateMaxCount() {  
    let newMaxCountValue = document.getElementById('new-max').value;  
    fetchJson(httpUrl + '/update-max?max=' + newMaxCountValue, 'PATCH', {})  
  
    document.getElementById('new-max').value = null;  
    return false; // 기본 양식 제출 방지  
}  
  
// 재실 인원 증가 함수  
function increaseOccupancy() {  
    let newOccupancy = document.getElementById('increase-occupancy').value;  
  
    fetchJson(httpUrl + '/increase-occupancy?num=' + newOccupancy, 'PATCH', {})  
        .then(data => {  
            console.log('재실 인원 증가 완료 - 증가한 수치 : ', newOccupancy)  
        });  
  
    document.getElementById('increase-occupancy').value = null;  
}  
  
// 재실 인원 감소 함수  
function decreaseOccupancy() {  
    let newOccupancy = document.getElementById('decrease-occupancy').value;  
  
    fetchJson(httpUrl + '/decrease-occupancy?num=' + newOccupancy, 'PATCH', {})  
        .then(data => {  
            console.log('재실 인원 감소 완료 - 감소한 수치 : ', newOccupancy)  
        });  
  
    document.getElementById('decrease-occupancy').value = null;  
}  
  
// 운영 시간 변경 함수  
function updateOperationTime() {  
    let newStartOpenTime = document.getElementById('new-start-open').value;  
    let newStartCloseTime = document.getElementById('new-start-close').value;  
    let newEndOpenTime = document.getElementById('new-end-open').value;  
    let newEndCloseTime = document.getElementById('new-end-close').value;  
  
    // Promise Chain으로 첫번쨰 요청 처리 후 두번쨰 요청 실행  
    fetch(httpUrl + '/open-time?openTime=' + newStartOpenTime + ':' + newStartCloseTime, {method: 'PATCH'})  
        .then(response => response.text())  
        .then(data => {  
            console.log('운영 시작 시간 업데이트:', newStartOpenTime + ':' + newStartCloseTime);  
  
            // 두 번째 POST 요청 실행  
            return fetch(httpUrl + '/close-time?closeTime=' + newEndOpenTime + ':' + newEndCloseTime, {method: 'PATCH'});  
        })  
        .then(response => response.text())  
        .then(data => {  
            console.log('운영 종료 시간 업데이트:', newEndOpenTime + ':' + newEndCloseTime);  
        })  
        .catch(error => {  
            console.error('운영 시간 변경 오류:', error);  
        });  
  
    document.getElementById('new-start-open').value = null;  
    document.getElementById('new-start-close').value = null;  
    document.getElementById('new-end-open').value = null;  
    document.getElementById('new-end-close').value = null;  
  
    return false; // 기본 양식 제출 방지  
}  
  
// Relay URL 변경 함수  
function updateRelayUrl() {  
    let newUrl = document.getElementById('new-relay').value;  
  
    fetchJson(httpUrl + '/relay?url=' + newUrl, 'PATCH', {})  
        .then(() => console.log("Relay URL 변경 - ", newUrl));  
  
    document.getElementById('new-relay').value = null;  
}  
  
/* 데이터 값 출력 함수들 */  
// 운영 시작 시간  
function displayOperationOpenTime(openTime) {  
    document.getElementById('open').innerText = openTime;  
}  
  
// 운영 종료 시간  
function displayOperationCloseTime(closeTime) {  
    document.getElementById('close').innerText = closeTime;  
}  
  
// 최대 인원  
function displayMaxCount(max) {  
    document.getElementById('max').innerText = max;  
}  
  
// Relay URL  
function displayRelayUrl(url) {  
    document.getElementById('url').innerText = url;  
}  
  
// 방안의 현재 인원  
function displayOccupancy(occupancy) {  
    document.getElementById('count').innerText = occupancy;  
}  
  
// 방안의 상태  
function displayStatus(status) {  
    document.getElementById('status').textContent = status;  
}  
  
/* --- Utility 함수 --- */
function fetchJson(url, method = 'GET') {  
    return window.fetch(url, {method, headers: {'Content-Type': 'application/json'}})  
        .then(response => response.json());  
}  
  
function fetchText(url, method = 'PATCH', body = {}) {  
    return window.fetch(url, {method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)})  
        .then(response => response.text());  
}
```

<br>

> 📌 **style.css**

1개의 CSS를 2개의 HTML에서 사용하고 있습니다.

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
  
body {  
    background-color: #001228;  
    /*background-image: url(back.png);*/  
    display: flex;  
    flex-direction: column;  
    align-items: center; /* 수직 정렬 (가운데) */  
    justify-content: center; /* 수평 정렬 (가운데) */  
    height: 100vh; /* 화면 높이에 맞추어 정렬 */    
    overflow: hidden;  
}  
  
/* Input */  
input[type="text"],  
input[type="number"] {  
    padding: 8px;  
    border-radius: 4px;  
}  
  
section {  
    text-align: center;  
}  
  
/* Div */  
div {  
    text-align: center;  
    margin-bottom: .8rem;  
    padding: .8rem;  
    border-radius: .3rem;  
    box-shadow: .1rem .1rem .3rem rgba(0, 0, 0, .2);  
}  
  
/* Span */  
span {  
    font-weight: bold  
}  
  
/* Paragraph */  
p {  
    font-size: 18px  
}  
  
.admin-body {  
    background-color: #001228;  
    /*background-image: url(back.png);*/  
    display: flex;  
    flex-direction: column;  
    align-items: center; /* 수직 정렬 (가운데) */  
    justify-content: center; /* 수평 정렬 (가운데) */  
    height: 100%; /* 화면 높이에 맞추어 정렬 */    
    overflow: auto;  
}  
  
.status-img {  
    width: 250px;  
    height: 250px;  
}  
  
.view {  
    color: white;  
    font-size: 40px;  
}  
  
.status {  
    font-size: 110px;  
    font-weight: bold;  
}  
  
.text-occupancy {  
    vertical-align: top;  
    background-color: white;  
    padding: 7px;  
    color: #001228;  
    border-radius: 10px;  
    font-size: 70px;  
    width: 280px;  
    height: 160px;  
    font-weight: bold;  
}  
  
.text-max {  
    vertical-align: top;  
    background-color: white;  
    padding: 7px;  
    color: #001228;  
    border-radius: 10px;  
    font-size: 70px;  
    width: 280px;  
    height: 160px;  
    font-weight: bold;  
}  
  
.flex-container {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: row;  
    margin: 30px;  
}  
  
.flex-item1 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-top: 20px;  
    height: 10vh;  
    padding-top: 50px;  
    margin-right: 30px;  
}  
  
.flex-item2 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-top: 20px;  
    height: 10vh;  
    padding-top: 50px;  
}  
  
.time {  
    width: 38px;  
}  
  
.admin-color {  
    color: white;  
}  
  
.admin-container1 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-left: 40px;  
}  
  
.admin-container2 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-left: 40px;  
    padding: 40px;  
}
```