package org.linkwave.ws.websocket.routing.args;

import org.linkwave.ws.websocket.routing.*;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;

import java.util.List;
import java.util.Map;

/**
 * Describes the way {@link WebSocketRouter} should resolve route handler's parameters
 * in order to construct his successful invocation. The message has a chance to be handled only if
 * it passed this stage, i.e. path that specified in message is valid and payload type is compatible with
 * the type of {@link Payload} declared in target {@link Endpoint} (route handler).
 */
public interface RouteHandlerArgumentResolver {

    /**
     * @param matchedRoute  defined route
     * @param pathVariables resolved parts of defined route
     * @param message       need to handle
     * @param session       of message initiator
     * @return non-null arguments list
     * @throws InvalidMessageFormatException if the message has invalid structure such as
     *                                       path representation or payload type, etc.
     * @throws InvalidPathException          if path is unavailable to parse
     */
    @Deprecated
    default List<Object> resolve(@NonNull Map.Entry<String, RouteComponent> matchedRoute,
                                 @NonNull Map<String, String> pathVariables,
                                 @NonNull RoutingMessage message,
                                 @NonNull WebSocketSession session
    ) throws InvalidMessageFormatException, InvalidPathException {
        return resolve(
                new MessageContext(
                        matchedRoute,
                        pathVariables,
                        message,
                        session
                )
        );
    }

    List<Object> resolve(@NonNull MessageContext context) throws InvalidMessageFormatException, InvalidPathException;

}
