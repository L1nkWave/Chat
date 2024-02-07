package com.chat.wsserver.websocket.routing;

import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;
import com.chat.wsserver.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;

import java.util.List;
import java.util.Map;

/**
 * Describes the way {@link WebSocketRouter} should resolve route handler's parameters
 * in order to construct his successful invocation. The message has a chance to be handled only if
 * it passed this stage, i.e. path that specified in message is valid and payload type is compatible with
 * the type of {@link Payload} declared in target {@link SubRoute} (route handler).
 */
public interface RouteHandlerArgumentResolver {

    /**
     *
     * @param matchedRoute defined route
     * @param pathVariables resolved parts of defined route
     * @param message need to handle
     * @param session of message initiator
     * @return non-null arguments list
     * @throws InvalidMessageFormatException if the message has invalid structure such as
     * path representation or payload type, etc.
     * @throws InvalidPathException if path is unavailable to parse
     */
    List<Object> resolve(@NonNull Map.Entry<String, RouteComponent> matchedRoute,
                         @NonNull Map<String, String> pathVariables,
                         @NonNull RoutingMessage message, @NonNull WebSocketSession session
    ) throws InvalidMessageFormatException, InvalidPathException;

}
