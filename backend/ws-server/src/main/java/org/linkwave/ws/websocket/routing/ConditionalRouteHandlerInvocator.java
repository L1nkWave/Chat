package org.linkwave.ws.websocket.routing;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.exception.ConditionViolatedException;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.util.ReflectionUtils.invokeMethod;

@Component
@RequiredArgsConstructor
public class ConditionalRouteHandlerInvocator implements RouteHandlerInvocator {

    private final RouteHandlerArgumentResolver argumentResolver;

    @Override
    public Object delegateInvocation(@NonNull MessageContext context)
            throws ConditionViolatedException, InvalidPathException, InvalidMessageFormatException {

        final RouteComponent route = context.route().getValue();
        route.conditions().forEach(condition -> condition.check(context));

        // prepare arguments for route handler invocation
        final List<Object> arguments = argumentResolver.resolve(context);
        return invokeMethod(route.routeHandler(), route.beanRoute(), arguments.toArray());
    }

}
