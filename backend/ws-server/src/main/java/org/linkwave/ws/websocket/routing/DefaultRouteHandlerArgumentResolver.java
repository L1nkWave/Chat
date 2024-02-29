package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.exception.RoutingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class DefaultRouteHandlerArgumentResolver implements RouteHandlerArgumentResolver {

    private final ObjectMapper objectMapper;

    @Override
    public List<Object> resolve(@NonNull Map.Entry<String, RouteComponent> matchedRoute,
                                @NonNull Map<String, String> pathVariables,
                                @NonNull RoutingMessage message,
                                @NonNull WebSocketSession session
    ) throws InvalidMessageFormatException, InvalidPathException {

        final Method routeHandler = matchedRoute.getValue().routeHandler();
        final String payload = message.payload();
        final List<Object> arguments = new ArrayList<>();

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
                            payload : objectMapper.readValue(payload, paramType);
                } catch (JsonProcessingException e) {
                    throw new InvalidMessageFormatException(
                            format("invalid json payload for route [%s]", matchedRoute.getKey())
                    );
                }

            } else if (param.isAnnotationPresent(PathVariable.class)) {

                String varName = param.getAnnotation(PathVariable.class).value();
                varName = varName.isBlank() ? param.getName() : varName;
                final String varValue = pathVariables.get(varName);

                if (varValue == null) {
                    throw new RoutingException(
                            format("\n\tNot found path variable with name \"%s\" in route[%s]",
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
                    } else if (paramType.equals(String.class)){
                        requiredArgument = varValue;
                    } else {
                        throw new RoutingException(
                                format("\n\tType \"%s\" is not supported for path variables", paramType.getName())
                        );
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

}
