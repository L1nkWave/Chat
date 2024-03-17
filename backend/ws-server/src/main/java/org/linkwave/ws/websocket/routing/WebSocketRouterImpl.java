package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRouterImpl implements WebSocketRouter {

    private static final String PATH_DELIMITER = "/";
    private static final String PATH_VAR_PREFIX = "{";
    private static final String PATH_VAR_POSTFIX = "}";

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

        RouteComponent route = matchedRoute.getValue();
        Method routeHandler = route.routeHandler();

        // prepare arguments for route handler invocation
        final List<Object> arguments = argumentResolver.resolve(
                matchedRoute, pathVariables, routingMessage, session
        );

        // invoke route handler
        Object invocationResult = invokeMethod(routeHandler, route.beanRoute(), arguments.toArray());

        if (invocationResult == null) {
            return;
        }

        final String jsonMessage;
        try {
            boolean isErrorResult = false;

            // handle special return type in route handler
            if (invocationResult instanceof Box<?> box) {
                isErrorResult = box.hasError();
                invocationResult = box.hasError() ? box.getErrorValue() : box.getValue();
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

        broadcastManager.process(routeHandler, pathVariables, jsonMessage);
    }

    @Nullable
    private Entry<String, RouteComponent> findRouteByPath(@NonNull String requiredPath,
                                                          @NonNull Map<String, String> pathVariables) {

        String[] targetPath = requiredPath.trim().split(PATH_DELIMITER);

        // iterate through registered routes until one will be found
        for (Entry<String, RouteComponent> entry : routes.entrySet()) {

            String[] path = entry.getKey().split(PATH_DELIMITER);
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
                    String pathVarName = path[i].substring(1, path[i].length() - 1);
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

    private boolean isPathVariable(@NonNull String s) {
        return s.startsWith(PATH_VAR_PREFIX) && s.endsWith(PATH_VAR_POSTFIX);
    }

}
