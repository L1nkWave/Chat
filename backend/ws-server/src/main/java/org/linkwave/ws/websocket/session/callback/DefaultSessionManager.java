package org.linkwave.ws.websocket.session.callback;

import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.session.AbstractSessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Slf4j
@Component
public class DefaultSessionManager extends AbstractSessionManager {

    private final List<AfterConnectionEstablished> connectedCallbacks;
    private final List<AfterConnectionClosed> disconnectedCallbacks;

    public DefaultSessionManager(List<AfterConnectionEstablished> connectedCallbacks,
                                 List<AfterConnectionClosed> disconnectedCallbacks,
                                 @Value("${ws.session.exp}") Long sessionExpiration) {
        super(sessionExpiration);
        this.connectedCallbacks = connectedCallbacks;
        this.disconnectedCallbacks = disconnectedCallbacks;
    }

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
