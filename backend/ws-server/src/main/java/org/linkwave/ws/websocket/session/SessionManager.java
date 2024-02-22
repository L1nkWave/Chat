package org.linkwave.ws.websocket.session;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * The key interface for dealing with web-socket sessions
 */
public interface SessionManager {

    /**
     * Used to save session to session context (i.e. sessions map).
     * @param session that needs to be saved to session context.
     */
    void persist(@NonNull WebSocketSession session);

    /**
     * Used to remove session from session context.
     * @param session that needs to be removed from session context.
     */
    void remove(@NonNull WebSocketSession session);

    /**
     * Used to find session by its id in session context.
     * @param sessionId not null id for which the session is to be returned
     * @return session if it is found, otherwise null
     */
    @Nullable WebSocketSession find(@NonNull String sessionId);

    /**
     * Used to retrieve map with all sessions attached to the current instance.
     * @return non-null thread-safe map
     */
    Map<String, WebSocketSession> getSessionContext();

}
