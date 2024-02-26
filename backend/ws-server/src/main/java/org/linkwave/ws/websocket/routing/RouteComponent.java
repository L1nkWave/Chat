package org.linkwave.ws.websocket.routing;

import java.lang.reflect.Method;

import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;

/**
 * Just a convenient container that holds reference to object or bean annotated
 * with {@link WebSocketRoute} is responsible for handling several routes and
 * one of handler that's declared in that bean.
 *
 * @param beanRoute    component with route handlers declaration
 * @param routeHandler one of the declared handlers ({@link SubRoute})
 */
public record RouteComponent(Object beanRoute, Method routeHandler) {
}
