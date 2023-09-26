## 📘 **현황판용 페이지 - 데이터 동기화**

현황판에 표시될 현재 입장 가능인원, 관리자용 최대인원 & 상태 수정 페이지를 따로 나눴습니다.

URL별 분리를 위해 간단한 컨트롤러를 하나 만들어서,

`/admin` 으로 들어오는 요청은 Static 디렉터리 밑에 admin.html로 포워딩 시켜줍니다.

**ViewController**

```java
// 어드민 페이지 분리를 위한 컨트롤러  
@Controller  
public class ViewController {  
    @RequestMapping("/admin")  
    public String adminPage() {  
        return "forward:/admin.html";  
    }  
}
```

<br>

> **현황판용** : `index.html, index.js`

- 현황판용에는 현재 xx실 출입 가능인원, 현재 인원이 출력됩니다.
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
  
<body>  
  
<br>  
  
<section>  
    <span id="status" style="font-size: 65px; font-weight: normal;"></span>  
</section>  
  
<br>  
  
<section class="flex-container">  
        <h2 class="view">현재 이용 인원</h2>  
        <p><span id="count" class="text-occupancy"></span></p>  
  
        <h2 class="view">최대 입장 가능 인원</h2>  
        <span id="max" class="text-max"></span>  
</section>  
  
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
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount    
    maxCount: 0, // 최대 수용 인원  
    status: "", // Room 상태 (Spring Enum : Status)    
    customStatus: "", // Custom Status  
    openTime: "",  
    closeTime: "",  
}  
  
stompClient.connect({}, (frame) => {  
    console.log('Connected: ' + frame);  
  
    stompClient.subscribe('/count/data', function (data) {  
        let entity = JSON.parse(data.body);  
        updateRoomInfo(entity);  
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
});  
  
// 렌더링 시 Entity 값 화면에 출력  
window.onload = function () {  
    loadInitialData();  
};  
  
function loadInitialData() {  
    fetchJson(httpUrl + '/init')  
        .then(data => {  
            updateRoomInfo(data);  
        });  
}  
  
// 현재 인원 업데이트 함수  
function updateRoomInfo(data) {  
    roomInfo.maxCount = data.maxCount;  
    roomInfo.customStatus = data.customStatus;  
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
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);  
            break;  
        case "MEDIUM":  
            roomInfo.status  = "혼잡합니다.";  
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);  
            break;  
        case "HIGH":  
            roomInfo.status  = "만실입니다.";  
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);  
            break;  
        case "NOT_OPERATING":  
            roomInfo.status  = "운영시간이 아닙니다.";  
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);  
            break;  
    }  
    displayMaxCount(roomInfo.maxCount);  
    displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);  
}  
  
// 최대 인원  
function displayMaxCount(max) {  
    document.getElementById('max').innerText = max;  
}  
  
// 방안의 현재 인원  
function displayOccupancy(occupancy) {  
    document.getElementById('count').innerText = occupancy;  
}  
  
// 방안의 상태  
function displayStatus(status, occupancy, maxCount) {  
    document.getElementById('status').innerText = status;  
    let coloredStatus = document.getElementById('status');  
  
    if (occupancy <= 9) {  
        coloredStatus.style.color = 'lawngreen';  
    } else if (occupancy >= 10 && occupancy < 15) {  
        coloredStatus.style.color = 'yellow';  
    } else if (occupancy >= maxCount) {  
        coloredStatus.style.color = 'red';  
    }  
  
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