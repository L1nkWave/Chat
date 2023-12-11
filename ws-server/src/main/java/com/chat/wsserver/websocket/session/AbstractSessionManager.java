package com.chat.wsserver.websocket.session;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSessionManager implements SessionManager {

    protected final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Map<String, WebSocketSession> getSessionContext() {
        return sessions;
    }

}
