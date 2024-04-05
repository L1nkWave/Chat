package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.exception.RoutingException;
import org.linkwave.ws.websocket.routing.parser.MessageParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.linkwave.ws.utils.RouteUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRouterImpl implements WebSocketRouter {

    private Map<String, RouteComponent> routes;
    private final MessageParser messageParser;
    private final RouteHandlerArgumentResolver argumentResolver;
    private final ObjectMapper mapper;
    private final BroadcastManager broadcastManager;

    @Override
    public void route(String message, WebSocketSession session) throws InvalidMessageFormatException, InvalidPathException {
        log.debug("-> route()");

        final RoutingMessage routingMessage = messageParser.parse(message);
        final Map<String, String> pathVariables = new HashMap<>();

        Entry<String, RouteComponent> matchedRoute = findRouteByPath(routingMessage.path(), pathVariables);
        if (matchedRoute == null) {
            throw new InvalidPathException("route not found");
        }

        final RouteComponent route = matchedRoute.getValue();
        final Method routeHandler = route.routeHandler();

        // create message context
        final var messageContext = new MessageContext(matchedRoute, pathVariables, routingMessage, session);

        // prepare arguments for route handler invocation
        final List<Object> arguments = argumentResolver.resolve(messageContext);

        // invoke route handler
        Object invocationResult;
        try {
            invocationResult = invokeMethod(routeHandler, route.beanRoute(), arguments.toArray());
        } catch (Exception e) {
            throw new RoutingException("An error occurred while routing message", e);
        }

        if (invocationResult == null) {
            return;
        }

        final String jsonMessage;
        try {
            boolean isErrorResult = false;

            // handle special return type in route handler
            if (invocationResult instanceof Box<?> box) {
                if (box.isEmpty()) {
                    return;
                }
                isErrorResult = box.hasError();
                invocationResult = isErrorResult ? box.getErrorValue() : box.getValue();
            }

            jsonMessage = mapper.writeValueAsString(invocationResult);

            // send message only to initiator if it is an error or not for broadcast purpose
            if (isErrorResult || !broadcastManager.isBroadcast(routeHandler)) {
                session.sendMessage(new TextMessage(jsonMessage));
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        broadcastManager.process(routeHandler, pathVariables, invocationResult, jsonMessage);
    }

    @Nullable
    private Entry<String, RouteComponent> findRouteByPath(@NonNull String requiredPath,
                                                          @NonNull Map<String, String> pathVariables) {

        final String[] targetPath = requiredPath.trim().split(ROUTE_DELIMITER);

        // iterate through registered routes until one will be found
        for (Entry<String, RouteComponent> entry : routes.entrySet()) {

            final String[] path = entry.getKey().split(ROUTE_DELIMITER);
            if (targetPath.length != path.length) {
                continue;
            }

            // compare parts of two paths
            boolean isMatched = true;
            for (int i = 0; i < path.length; i++) {

                if (targetPath[i].equals(path[i])) {
                    continue;
                }

                if (isPathVariable(path[i])) {
                    // remove {, }. Example: {id} => id
                    String pathVarName = getPathVariable(path[i]);
                    pathVariables.put(pathVarName, targetPath[i]);
                } else {
                    isMatched = false;
                    break;
                }

            }

            if (isMatched) {
                return entry;
            }

            // clear path vars if route does not match
            pathVariables.clear();

        }
        return null;
    }

}
