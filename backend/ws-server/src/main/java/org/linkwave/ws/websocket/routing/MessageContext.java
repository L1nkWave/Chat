package org.linkwave.ws.websocket.routing;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @param route defined route
 * @param pathVariables resolved parts of defined route
 * @param routingMessage raw message is needed to be handled
 * @param session message initiator
 */
public record MessageContext(
        Entry<String, RouteComponent> route,
        Map<String, String> pathVariables,
        RoutingMessage routingMessage,
        WebSocketSession session) {
}
