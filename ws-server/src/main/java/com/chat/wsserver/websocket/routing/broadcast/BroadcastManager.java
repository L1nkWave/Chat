package com.chat.wsserver.websocket.routing.broadcast;

import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.Map;

import com.chat.wsserver.websocket.routing.bpp.Broadcast;

/**
 * Built-in component that is used by router to automatically broadcast message
 * using {@code @Broadcast} annotation
 *
 * @see Broadcast
 */
public interface BroadcastManager {

    String KEY_SEPARATOR = ":";

    /**
     *
     * @param routeHandler object with corresponding route
     * @param jsonMessage serialized message to deliver
     */
    void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables, String jsonMessage);
}
