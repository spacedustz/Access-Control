## 📘 **index.html**

- Spring Boot 내부 resource/static 디렉터리 내부에 index.html을 만들어 주었습니다.
- Spring WebSocket config에 작성 해놓은 소켓 URL인 `ws`를 `ws://localhost:8090` 뒤에 붙여 `ws://localhost:8090/ws`로 웹 소켓에 연결해줍니다.
- 그리고, Spring에서 RabbitTopicListener 로직 맨 밑에 있던 convertAndSend() 함수에 써놨던 Subscribe URL을 stompClient.subscribe에 넣어줍니다. (`/count/data`)
- 그럼 Event 객체가 브라우저로 넘어옵니다.
- 이 넘어온 Event 객체를 자유롭게 HTML의 body 부분에 쓸 필드를 지정해 사용해서 화면에 출력합니다.

```html
<!DOCTYPE html>  
<html>  
<head>  
    <meta charset="UTF-8">  
    <title>입장 인원 카운트</title>  
  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.4/sockjs.min.js"></script>  
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>  
  
    <script type="text/javascript">   
        var socket = new WebSocket('ws://localhost:8090/ws');  
        var stompClient = Stomp.over(socket);  
  
        stompClient.connect({}, function(frame) {  
            console.log('Connected: ' + frame);  
            stompClient.subscribe('/count/data', function(data){  
                var eventData = JSON.parse(data.body); // 실시간 데이터 파싱  
                updateCount(eventData);
            });  
        });  
  
        // 실시간으로 데이터를 업데이트 하는 함수  
        function updateCount(eventData) {
            document.getElementById('count').innerText = eventData.occupancy + " / " + eventData.maxCount;  
            document.getElementById('status').innerText = eventData.status;  
        }  
    </script>  
  
</head>  
<body>  
  
<div align="center">  
    <div>  
        <h1>현재 : <span id="status"></span></h1>  
    </div>  
  
    <div>  
        <h2>입장 가능 인원: <span id="count" class="text-center"></span></h2>  
    </div>  
</div>  
  
</body>  
</html>
```

<br>

Spring의 소켓에 접속해 Event 객체를 받아 객체의 값을 임의로 사용하기

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-sock.png)