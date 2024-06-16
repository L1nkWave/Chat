package org.linkwave.ws.websocket.routing.args;

import org.linkwave.ws.websocket.routing.MessageContext;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.lang.NonNull;

import java.lang.reflect.Parameter;

public interface ArgumentResolverStrategy {

    Object resolve(@NonNull MessageContext context, @NonNull Parameter param)
            throws InvalidMessageFormatException, InvalidPathException;

}
