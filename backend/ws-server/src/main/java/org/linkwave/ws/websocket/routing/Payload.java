package org.linkwave.ws.websocket.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;

/**
 * Used to indicate parameter as message payload in {@link SubRoute} (route handler).
 * Moreover, the annotated parameter is involved in {@link RouteHandlerArgumentResolver}
 * in message injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Payload {
}
