package com.accesscontrol.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static org.springframework.messaging.simp.stomp.StompHeaders.SESSION;

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
