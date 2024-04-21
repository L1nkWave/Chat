package org.linkwave.ws.websocket.routing.bpp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for {@link Broadcast} annotations in order to
 * support broadcast for different destinations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Broadcasts {

    Broadcast[] value() default {};

}
