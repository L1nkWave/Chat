package com.chat.wsserver.websocket.session.callback;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

/**
 * Callback that is invoked when user connected to server.
 */
@FunctionalInterface
public interface AfterConnectionEstablished {
    void afterConnected(@NonNull WebSocketSession session);
}
