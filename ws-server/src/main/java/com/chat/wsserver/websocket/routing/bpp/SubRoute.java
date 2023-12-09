package com.chat.wsserver.websocket.routing.bpp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods of {@code @WebSocketRoute} beans
 * aimed to handle received message by defined route at value property
 *
 * @see WebSocketRoute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubRoute {
    String value();
    boolean disabled() default false;
}
