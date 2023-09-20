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

/* --- Utility 함수 --- */
function fetchJson(url, method='GET') {
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