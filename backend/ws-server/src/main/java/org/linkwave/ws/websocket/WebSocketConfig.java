package org.linkwave.ws.websocket;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.websocket.jwt.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RootWebSocketHandler rootHandler;
    private final HandshakeHandler handshakeHandler;
    private final HandshakeInterceptor handshakeInterceptor;

    /** Chỉ chấp nhận kết nối từ các origin của frontend (ws.allowed-origins, đặt bằng WS_ALLOWED_ORIGINS). */
    @Value("${ws.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(rootHandler, JwtHandshakeInterceptor.HANDSHAKE_PATH)
                .addInterceptors(handshakeInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOrigins(allowedOrigins);
    }

}