package org.linkwave.ws.websocket.routing.bpp;

import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to enable message broadcast for route handler
 *
 * @see BroadcastManager
 * @see SubRoute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Broadcast {

    /**
     * Value contains a key-pattern that is used in Redis
     * to find users session ids.
     * For example, suppose we have pattern {@code chat:{id}}, where "chat" is just part of the key
     * and "id" is a name of path variable from route that is specified in {@code SubRoute} annotation
     * of specific route handler
     */
    String value();
    boolean multiInstances() default false;
}
