package com.chat.wsserver.websocket.session.callback;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

/**
 * Callback that is invoked when user disconnected from server.
 */
@FunctionalInterface
public interface AfterConnectionClosed {
    void afterDisconnected(@NonNull WebSocketSession session);
}
