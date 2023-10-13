## 📘 **WebSocketConfig**

`WebSocketMessageBrokerConfigurer` : 인터페이스를 구현해 STOMP로 메시지 처리 구성합니다.

**configureMessageBroker() 함수** : 메시지를 중간에서 라우팅할 때 사용하는 메시지 브로커를 구성하는 함수입니다.
- 보통 `/topic`, `/queue`를 사용합니다.
- `/topic`은 한명이 Message를 발행했을 때 해당 토픽을 구독하고 있는 N명에게 메시지를 브로드캐스팅 할 때 사용합니다.
- `/queue`는 한명이 Message를 발행했을 때 발행한 1명에게 다시 정보를 보내는 경우에 사용합니다.
  `enableSimpleBroker`
- 해당 주소를 구독하는 클라이언트에게 메시지를 보냅니다.
- 즉, 인자에는 구독 요청의 prefix를 넣고, 클라이언트에서 1번 채널을 구독하고자 할 때는 /sub/1 형식과 같은 규칙을 따라야 합니다.
  `setApplicationDestinationPrefixes`
- 메시지 발행 요청의 prefix를 넣습니다.
- `/`로 시작하는 메시지만 해당 Broker에서 받아서 처리하고, 클라이언트에서 WebSocket에 접속할 수 있는 endpoint를 지정합니다.
- 만약 `/app`으로 설정한다면, 실제 구독 신청 URL은 `/app/topic`처럼 시작 URL을 지정합니다.
- 아래 파일에서는 /count 로 구독 요청을 보내면 소켓과 연결 됩니다.

<br>

**registerStompEndpoints() 함수** : Socket Endpoint를 등록하는 함수입니다.
- `ws`라는 Endpoint에 Interceptor를 추가해 Socket을 등록합니다.
- 인터셉터는 바로 아래에 설명 하겠습니다.

```java
@Configuration  
@RequiredArgsConstructor  
@EnableWebSocketMessageBroker  
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {  
  
    @Override  
    public void configureMessageBroker(MessageBrokerRegistry registry) {  
        registry.enableSimpleBroker("/count");  
        registry.setApplicationDestinationPrefixes("/");  
    }  
  
    @Override  
    public void registerStompEndpoints(StompEndpointRegistry registry) {  
        registry.addEndpoint("/ws")  
                .setAllowedOrigins("*")  
                .addInterceptors(new HttpSessionHandshakeInterceptor());  
    }  
}
```

---

## 📘 **HttpHandshakeInterceptor**

WebSocket 연결을 수립하기 전에 `beforeHandshake()` 함수가 실행됩니다.
- 웹소켓은 처음 Connect 시점에 Handshake라는 작업이 수행됩니다.
- Handshake 과정은 HTTP 통신 기반으로 이루어지며 GET 방식으로 통신을 하게 됩니다.
- 이때, HTTTP Request Header의 Connection 속성은 Upgrade로 되어야 합니다.
- HTTP에 존재하는 Session을 WebSocket Session으로 등록합니다.
- SESSION 변수는 static 변수로 String 타입입니다.

```java
public class HttpHandshakeInterceptor implements HandshakeInterceptor {  
    @Override  
    public boolean beforeHandshake(ServerHttpRequest request,  
                                   ServerHttpResponse response,  
                                   WebSocketHandler wsHandler,  
                                   Map<String, Object> attributes) throws Exception {  
        if (request instanceof ServletServerHttpRequest servletRequest) {
		        // 현재 요청이 HTTP 요청인 경우, HTTP 세션을 가져옵니다.
            HttpSession session = servletRequest.getServletRequest().getSession();  
            // 가져온 HTTP 세션을 WebSocket 연결과 연관된 속성(attributes)으로 저장합니다.
            attributes.put(SESSION, session);  
        }
        // true를 반환하면 WebSocket 연결이 계속 진행되고, false를 반환하면 연결이 중단됩니다.
        return true;  
    }  

		// WebSocket 연결이 수립된 후에 실행됩니다.
		// 주로 예외 처리나 추가 작업을 수행하는 데 사용됩니다.
    @Override  
    public void afterHandshake(ServerHttpRequest request,  
                               ServerHttpResponse response,  
                               WebSocketHandler wsHandler,  
                               Exception exception) {}  
}
```