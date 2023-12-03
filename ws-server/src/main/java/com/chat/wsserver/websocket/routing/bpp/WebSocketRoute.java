package com.chat.wsserver.websocket.routing.bpp;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to define bean that can hold
 * route handlers, i.e. methods marked with {@code @SubRoute}
 *
 * @see SubRoute
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebSocketRoute {
    String value();
}
