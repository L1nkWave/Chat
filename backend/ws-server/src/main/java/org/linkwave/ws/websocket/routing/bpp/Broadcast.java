package org.linkwave.ws.websocket.routing.bpp;

import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;

import java.lang.annotation.*;

/**
 * This annotation is used to enable message broadcast for route handler.
 *
 * @see BroadcastManager
 * @see Endpoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Broadcasts.class)
public @interface Broadcast {

    /**
     * Value contains a key-pattern that is used in Redis
     * to find users session ids.<br/>
     * For example, suppose we have the next pattern <b>{@code chat:{id}}</b>, where "chat" is just semantic part of the key
     * and "id" is a name of:
     *
     * <ul>
     *     <li>
     *          a path variable from endpoint that is specified in {@link  Endpoint} annotation that
     *          attached to specific route handler.
     *      </li>
     *      <li>
     *          a field from received message for broadcast. In order to use this feature the {@link Broadcast#analyzeMessage()}
     *          should be set to {@code true}.
     *      </li>
     * </ul>
     */
    String value() default "chat:{id}";

    /**
     * Specifies whether {@link BroadcastManager} should be intended to use message content
     * to resolve defined key in {@link Broadcast#value()} or not.
     */
    boolean analyzeMessage() default false;

    /**
     * The message will be broadcasted to other instances if set to {@code true}, <br/>
     * otherwise only locally.
     */
    boolean multiInstances() default true;
}
