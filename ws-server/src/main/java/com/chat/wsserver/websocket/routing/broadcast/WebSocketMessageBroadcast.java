package com.chat.wsserver.websocket.routing.broadcast;

import org.springframework.lang.NonNull;

import java.util.Set;

public interface WebSocketMessageBroadcast {

    /**
     * @param sessionIds ids of connected users that is needed to be delivered message to
     * @param json       serialized object (message) that should be delivered
     * @return true      if all users received message (all session are present in current instance)
     */
    boolean share(@NonNull Set<String> sessionIds, String json);

}
