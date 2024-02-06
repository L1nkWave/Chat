package com.chat.wsserver.websocket.routing.broadcast;

import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Set;

/**
 * Core component under the hood in {@link BroadcastManager} that does low-level job
 * to deliver messages directly to clients via web socket sessions.
 *
 * @see org.springframework.web.socket.WebSocketSession
 */
public interface WebSocketMessageBroadcast {

    /**
     * @param sessionIds ids of connected clients that are needed to be delivered message to
     * @param json       serialized object (message) that should be delivered
     * @return true      if all users received message (all session are present in current instance)
     * @throws IOException when message sending is failed to at least one session
     */
    boolean share(@NonNull Set<String> sessionIds, String json) throws IOException;

}
