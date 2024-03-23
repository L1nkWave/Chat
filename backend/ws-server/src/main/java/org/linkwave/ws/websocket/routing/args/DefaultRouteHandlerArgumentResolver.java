package org.linkwave.ws.websocket.routing.args;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.websocket.routing.MessageContext;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultRouteHandlerArgumentResolver implements RouteHandlerArgumentResolver {

    private final List<ArgumentResolverStrategy> argResolverStrategies;

    @Override
    public List<Object> resolve(@NonNull MessageContext context)
            throws InvalidMessageFormatException, InvalidPathException {

        final Method routeHandler = context.route()
                .getValue()
                .routeHandler();

        final List<Object> arguments = new ArrayList<>();

        for (Parameter parameter : routeHandler.getParameters()) {

            Object requiredArgument = null;

            for (var strategy : argResolverStrategies) {
                final Object arg = strategy.resolve(context, parameter);
                if (arg != null) {
                    requiredArgument = arg;
                    break;
                }
            }
            arguments.add(requiredArgument);
        }
        return arguments;
    }

}
