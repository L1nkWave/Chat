package com.chat.wsserver.websocket.routing;

import java.lang.reflect.Method;

public record RouteComponent(Object beanRoute, Method routeHandler) {
}
