package org.linkwave.ws.websocket.session.callback;

import org.linkwave.ws.websocket.session.AbstractSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSessionManager extends AbstractSessionManager {

    private final List<AfterConnectionEstablished> connectedCallbacks;
    private final List<AfterConnectionClosed> disconnectedCallbacks;

    @Override
    public void persist(@NonNull WebSocketSession session) {
        sessions.put(session.getId(), session);
        connectedCallbacks.forEach(c -> c.afterConnected(session));
    }

    @Override
    public WebSocketSession find(@NonNull String sessionId) {
        return sessions.getIfPresent(sessionId);
    }

    @Override
    public void remove(@NonNull WebSocketSession session) {
        sessions.invalidate(session.getId());
        disconnectedCallbacks.forEach(c -> c.afterDisconnected(session));
    }

}
