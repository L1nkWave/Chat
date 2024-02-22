package org.linkwave.ws.websocket.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
public abstract class AbstractSessionManager implements SessionManager {

    protected final Cache<String, WebSocketSession> sessions = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10)) // access token time expiration
        .removalListener((String sessionId, WebSocketSession session, RemovalCause cause) -> {
            if (cause != RemovalCause.EXPIRED) {
                return;
            }
            log.debug("-> removalListener(): session={}", session);
            if (session != null) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException e) {
                    log.error("-> removalListener(): session can't be closed");
                    this.remove(session);
                }
            }
        })
        .build();

    @Override
    public Map<String, WebSocketSession> getSessionContext() {
        return sessions.asMap();
    }

}
