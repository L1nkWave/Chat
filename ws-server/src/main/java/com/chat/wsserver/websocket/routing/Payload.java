package com.chat.wsserver.websocket.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;

/**
 * Used to indicate parameter as message payload in {@link SubRoute} (route handler).
 * Moreover, the type of parameter with this annotation is involved
 * by {@link RouteHandlerArgumentResolver} in message injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Payload {
}
