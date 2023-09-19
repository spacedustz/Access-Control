> **index.html** 수정

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>입장 인원 카운트</title>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.4/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

    <script type="text/javascript">
        var socket = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);
            stompClient.subscribe('/topic/data', function(data){
                console.log(data.body); // 실시간 데이터 출력
                // 여기에 실시간으로 데이터를 화면에 업데이트하는 코드를 작성
            });
        });
    </script>

</head>
<body>

<div align="center">
<h1>입장 가능 인원: <span id="count">1/15</span></h1>
</div>

<script type="text/javascript">
    var socket = new WebSocket('ws://localhost:8080/ws/events');

    socket.onmessage = function(event) {
        document.getElementById('count').innerText = event.data;
    };
</script>

</body>
</html>