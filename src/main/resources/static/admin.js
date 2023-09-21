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

/* --- Utility 함수 --- */
function fetchJson(url, method='GET') {
    return window.fetch(url, { method , headers : {'Content-Type': 'application/json'}})
        .then(response => response.json());
}

function fetchText(url, method='PATCH', body={}) {
    return window.fetch(url,{method , headers : {'Content-Type': 'application/json'}, body : JSON.stringify(body)})
        .then(response => response.text());
}