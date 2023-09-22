const wsUrl = 'ws://localhost:8090/ws';
const httpUrl = 'http://localhost:8090/ws';

let socket = new WebSocket(wsUrl);
let stompClient = Stomp.over(socket);

let roomInfo = {
    id: null, // ID
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount
    maxCount: 0, // 최대 수용 인원
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

            displayOccupancy(roomInfo.occupancy);
            displayMaxCount(roomInfo.maxCount);
            displayStatus(roomInfo.status, roomInfo.occupancy);
        });
}

// 현재 인원 업데이트 함수
function updateRoomInfo(updatedData) {
    displayOccupancy(updatedData.occupancy);
    displayMaxCount(updatedData.maxCount);
    displayStatus(updatedData.status, updatedData.occupancy);
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
function displayStatus(status, occupancy) {
    document.getElementById('status').innerText = status;

    let coloredStatus = document.getElementById('status');

    if (occupancy <= 9) {
        coloredStatus.style.color = 'lawngreen';
    } else if (occupancy >= 10 && occupancy < 15) {
        coloredStatus.style.color = 'yellow';
    } else if (occupancy => 15) {
        coloredStatus.style.color = 'red';
    }
}

/* --- Utility 함수 --- */
function fetchJson(url, method='GET') {
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})
        .then(response => response.json());
}

function fetchText(url, method='PATCH', body={}) {
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})
        .then(response => response.text());
}