package org.linkwave.ws.websocket.routing.broadcast;

import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.Map;

import org.linkwave.ws.websocket.routing.bpp.Broadcast;

/**
 * Built-in component that is used by router to automatically broadcast message
 * using {@link Broadcast} annotation. Broadcast manager is informed about what, how and where
 * the message must be delivered.
 *
 * @see Broadcast
 */
public interface BroadcastManager {

    String KEY_SEPARATOR = ":";

    /**
     * @param routeHandler      object with corresponding route
     * @param serializedMessage text message to deliver
     */
    void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables,
                 @NonNull Object message, @NonNull String serializedMessage);

    boolean isBroadcast(@NonNull Method routeHandler);

}
