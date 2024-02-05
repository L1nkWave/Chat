package com.chat.wsserver.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

public interface WebSocketSessionConfigurer {
    WebSocketSession configure(@NonNull WebSocketSession session);
}
