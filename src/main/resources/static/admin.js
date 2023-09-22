const wsUrl = 'ws://localhost:8090/ws';
const httpUrl = 'http://localhost:8090/ws';

let socket = new WebSocket(wsUrl);
let stompClient = Stomp.over(socket);

let roomInfo = {
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount
    maxCount: 0, // 최대 수용 인원
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

// 렌더링 시, 초기 데이터 값 출력
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

/* --- Utility 함수 --- */
function fetchJson(url, method='GET') {
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})
        .then(response => response.json());
}

function fetchText(url, method='PATCH', body={}) {
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})
        .then(response => response.text());
}