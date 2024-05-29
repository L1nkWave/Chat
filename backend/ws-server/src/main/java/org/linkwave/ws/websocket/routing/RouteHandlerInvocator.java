package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;

@FunctionalInterface
public interface RouteHandlerInvocator {

    /**
     * Handles the router handler invocation process, including resolving route's arguments.
     *
     * @param context message context
     * @return handler invocation result
     * @throws InvalidPathException          if the message has invalid structure such as
     *                                       path representation or payload type, etc.
     * @throws InvalidMessageFormatException if path is unavailable to parse
     */
    Object delegateInvocation(@NonNull MessageContext context)
            throws InvalidPathException, InvalidMessageFormatException;

}
