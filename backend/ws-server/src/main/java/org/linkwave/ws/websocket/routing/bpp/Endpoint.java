package org.linkwave.ws.websocket.routing.bpp;

import org.linkwave.ws.websocket.routing.EndpointCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods of {@code @WebSocketRoute} beans that are
 * aimed to handle received messages for specific endpoint.
 *
 * @see WebSocketRoute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Endpoint {
    String value() default "";

    /**
     * Prevents route handler from registration.
     */
    boolean disabled() default false;

    /**
     * List of conditions that should be passed through to get route handler invoked.
     */
    Class<? extends EndpointCondition>[] conditions() default {};
}
