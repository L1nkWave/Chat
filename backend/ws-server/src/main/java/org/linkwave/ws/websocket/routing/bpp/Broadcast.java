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
     * to find users session ids.<br/>
     * For example, suppose we have the next pattern <b>{@code chat:{id}}</b>, where "chat" is just semantic part of the key
     * and "id" is a name of:
     *
     * <ul>
     *     <li>
     *          a path variable from route that is specified in {@link  SubRoute} annotation that
     *          attached to specific route handler.
     *      </li>
     *      <li>
     *          a field from received message for broadcast. In order to use this feature the {@code messageAnalysis}
     *          property should be set to {@code true}.
     *      </li>
     * </ul>
     */
    String value();

    /**
     * Specifies whether {@link BroadcastManager} should be intended to use message content
     * to resolve defined key in {@link Broadcast#value()} or not.
     */
    boolean analyzeMessage() default false;

    boolean multiInstances() default false;
}
