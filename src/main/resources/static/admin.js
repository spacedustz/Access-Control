const wsUrl = 'ws://localhost:8090/ws';
const httpUrl = 'http://localhost:8090/ws';

let socket = new WebSocket(wsUrl);
let stompClient = Stomp.over(socket);

let roomInfo = {
    occupancy: 0, // 현재 Room 내 인원 수 : InCount - OutCount
    maxCount: 0, // 최대 수용 인원
    status: "", // Room 상태 (Spring Enum : Status)
    customStatus: "", // Custom Status
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
            roomInfo.status  = "혼잡합니다.";
            displayStatus(roomInfo.status);
            break;
        case "HIGH":
            roomInfo.status  = "만실입니다.";
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