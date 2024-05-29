package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.exception.ConditionViolatedException;
import org.springframework.lang.NonNull;

@FunctionalInterface
public interface EndpointCondition {

    /**
     * Defines the condition that message should pass through
     * in order to get handled by route handler. The condition can be set up in {@link Endpoint#conditions()}.
     *
     * @param context message context
     * @throws ConditionViolatedException if condition is violated
     */
    void check(@NonNull MessageContext context) throws ConditionViolatedException;

}
