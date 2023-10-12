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
        let coloredStatus = document.getElementById('status');
        let statusImg = document.getElementById('statusImg');
        coloredStatus.style.color = '#ff0000';
        statusImg.src = './img/Red.png';

        console.log('Custom 상태 업데이트 : ', entity.customStatus);
    });

    stompClient.subscribe('/count/occupancy', function (data) {
        let entity = JSON.parse(data.body)
        updateRoomInfo(entity)
        console.log('재실 인원 업데이트 : ', entity.occupancy);
    });

    stompClient.subscribe('/count/time', function (data) {
        let entity = JSON.parse(data.body)
        updateRoomInfo(entity)
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
            roomInfo.status  = "조금 혼잡합니다.";
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);
            break;
        case "HIGH":
            roomInfo.status  = "입장이 불가합니다.";
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);
            break;
        case "NOT_OPERATING":
            roomInfo.status  = "운영시간이 아닙니다.";
            displayStatus(roomInfo.status, roomInfo.occupancy, roomInfo.maxCount);
            break;
    }
    displayMaxCount(roomInfo.maxCount);
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
    let statusImg = document.getElementById('statusImg');

    if (occupancy <= 9) {
        coloredStatus.style.color = '#1494ff';
        statusImg.src = './img/Blue.png';
        document.getElementById('view-occupancy').style.color = '#1494ff';
        document.getElementById('view-max').style.color = '#1494ff';
    } else if (occupancy >= 10 && occupancy < maxCount) {
        coloredStatus.style.color = '#E6EC20';
        statusImg.src = './img/Yellow.png';
        document.getElementById('view-occupancy').style.color = '#E6EC20';
        document.getElementById('view-max').style.color = '#E6EC20';
    } else if (occupancy >= maxCount) {
        coloredStatus.style.color = '#ff0000';
        statusImg.src = './img/Red.png';
        document.getElementById('view-occupancy').style.color = '#ff0000';
        document.getElementById('view-max').style.color = '#ff0000';
    }


    if (document.getElementById('status').innerText === '운영시간이 아닙니다.') {
        coloredStatus.style.color = '#ff0000';
        statusImg.src = './img/Red.png';
        document.getElementById('view-occupancy').style.color = '#ff0000';
        document.getElementById('view-max').style.color = '#ff0000';
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