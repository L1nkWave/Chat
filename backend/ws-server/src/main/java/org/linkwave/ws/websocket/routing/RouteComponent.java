package org.linkwave.ws.websocket.routing;

import java.lang.reflect.Method;
import java.util.List;

import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;

/**
 * Just a convenient container that holds reference to object or bean annotated
 * with {@link WebSocketRoute} is responsible for handling several routes and
 * one of handler that's declared in that bean.
 *
 * @param beanRoute    component with route handlers declaration
 * @param routeHandler one of the declared handlers ({@link Endpoint})
 * @param conditions   list of object that define conditions to invoke handler
 */
public record RouteComponent(
        Object beanRoute,
        Method routeHandler,
        List<EndpointCondition> conditions) {
}
