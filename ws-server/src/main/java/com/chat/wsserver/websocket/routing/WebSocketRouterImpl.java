package com.chat.wsserver.websocket.routing;

import com.chat.wsserver.websocket.routing.broadcast.BroadcastManager;
import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;
import com.chat.wsserver.websocket.routing.exception.InvalidPathException;
import com.chat.wsserver.websocket.routing.exception.RoutingException;
import com.chat.wsserver.websocket.routing.parser.MessageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.String.format;
import static org.springframework.util.ReflectionUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRouterImpl implements WebSocketRouter {

    private static final String PATH_DELIMITER = "/";
    private static final String PATH_VAR_PREFIX = "{";
    private static final String PATH_VAR_POSTFIX = "}";

    private Map<String, RouteComponent> routes;
    private final MessageParser messageParser;
    private final ObjectMapper mapper;
    private final BroadcastManager broadcastManager;

    @Override
    public void route(String message, WebSocketSession session) throws InvalidMessageFormatException, InvalidPathException {
        log.info("-> route()");

        RoutingMessage routingMessage = messageParser.parse(message);
        Map<String, String> pathVariables = new HashMap<>();

        Entry<String, RouteComponent> matchedRoute = findRouteByPath(routingMessage.path(), pathVariables);
        if (matchedRoute == null) {
            throw new InvalidPathException("route not found");
        }

        RouteComponent route = matchedRoute.getValue();
        Method routeHandler = route.routeHandler();

        // prepare arguments for route handler invocation
        List<Object> arguments = resolveRouteHandlerParams(
                session,
                routeHandler,
                routingMessage.payload(),
                pathVariables,
                matchedRoute
        );

        Object invocationResult = invokeMethod(routeHandler, route.beanRoute(), arguments.toArray());

        if (invocationResult == null) {
            return;
        }

        String jsonMessage;
        try {
            jsonMessage = mapper.writeValueAsString(invocationResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        broadcastManager.process(routeHandler, pathVariables, jsonMessage);
    }

    private List<Object> resolveRouteHandlerParams(
            WebSocketSession session, Method routeHandler, String payload,
            Map<String, String> pathVariables,
            Entry<String, RouteComponent> matchedRoute) throws InvalidMessageFormatException, InvalidPathException {

        List<Object> arguments = new ArrayList<>();

        for (Parameter param : routeHandler.getParameters()) {

            Class<?> paramType = param.getType();
            Object requiredArgument = null;

            if (paramType.equals(WebSocketSession.class)) {
                requiredArgument = session;
            } else if (param.isAnnotationPresent(Payload.class)) {

                if (payload == null) {
                    throw new InvalidMessageFormatException(
                            format("payload must not be empty for route [%s]", matchedRoute.getKey())
                    );
                }

                try {
                    requiredArgument = paramType.equals(String.class) ?
                            payload : mapper.readValue(payload, paramType);
                } catch (JsonProcessingException e) {
                    throw new InvalidMessageFormatException(
                            format("invalid json payload for route [%s]", matchedRoute.getKey())
                    );
                }

            } else if (param.isAnnotationPresent(PathVariable.class)) {

                String varName = param.getAnnotation(PathVariable.class).value();
                String varValue = pathVariables.get(varName);

                if (varValue == null) {
                    throw new RoutingException(
                            format("\n\tNot found path variable with name \"%s\" in route:\t%s\n",
                                    varName, matchedRoute.getKey()
                            )
                    );
                }

                // try parse path variable
                try {
                    if (paramType.equals(int.class)) {
                        requiredArgument = Integer.parseInt(varValue);
                    } else if (paramType.equals(long.class)) {
                        requiredArgument = Long.parseLong(varValue);
                    } else {
                        requiredArgument = varValue;
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidPathException(format("Path variable \"%s\" incorrect type", varName));
                }

            } else if (paramType.isPrimitive()) {
                // for any primitive parameter set default value
                requiredArgument = paramType.equals(boolean.class) ? false : 0;
            }
            arguments.add(requiredArgument);
        }
        return arguments;
    }

    private Entry<String, RouteComponent> findRouteByPath(String requiredPath, Map<String, String> pathVariables) {

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

    private boolean isPathVariable(String s) {
        return s.startsWith(PATH_VAR_PREFIX) && s.endsWith(PATH_VAR_POSTFIX);
    }

}
