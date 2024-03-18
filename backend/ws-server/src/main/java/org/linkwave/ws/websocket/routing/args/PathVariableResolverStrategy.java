package org.linkwave.ws.websocket.routing.args;

import org.linkwave.ws.websocket.routing.MessageContext;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.exception.RoutingException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Parameter;

import static java.lang.String.format;

public class PathVariableResolverStrategy implements ArgumentResolverStrategy {

    @Override
    public Object resolve(@NonNull MessageContext context, @NonNull Parameter param) throws InvalidPathException {

        if (!param.isAnnotationPresent(PathVariable.class)) {
            return null;
        }

        String varName = param.getAnnotation(PathVariable.class).value();
        varName = varName.isBlank() ? param.getName() : varName;
        final String varValue = context.pathVariables().get(varName);
        final Class<?> paramType = param.getType();

        if (varValue == null) {
            throw new RoutingException(
                    format("\n\tNot found path variable with name \"%s\" in route[%s]",
                            varName, context.route().getKey()
                    )
            );
        }

        try {
            if (paramType.equals(int.class)) {
                return Integer.parseInt(varValue);
            } else if (paramType.equals(long.class)) {
                return Long.parseLong(varValue);
            } else if (paramType.equals(String.class)) {
                return varValue;
            } else {
                throw new RoutingException(
                        format("\n\tType \"%s\" is not supported for path variables", paramType.getName())
                );
            }
        } catch (NumberFormatException e) {
            throw new InvalidPathException(format("Path variable \"%s\" incorrect type", varName));
        }
    }

}
