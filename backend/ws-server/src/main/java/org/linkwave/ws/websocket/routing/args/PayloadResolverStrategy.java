package org.linkwave.ws.websocket.routing.args;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.linkwave.ws.websocket.routing.MessageContext;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.springframework.lang.NonNull;

import java.lang.reflect.Parameter;

import static java.lang.String.format;

@RequiredArgsConstructor
public class PayloadResolverStrategy implements ArgumentResolverStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object resolve(@NonNull MessageContext context, @NonNull Parameter param)
            throws InvalidMessageFormatException {

        if (!param.isAnnotationPresent(Payload.class)) {
            return null;
        }

        final Class<?> paramType = param.getType();
        final String route = context.route().getKey();
        final String payload = context.routingMessage().payload();

        if (payload == null) {
            throw new InvalidMessageFormatException(format("payload must not be empty for route [%s]", route));
        }

        try {
            return paramType.equals(String.class)
                    ? payload
                    : objectMapper.readValue(payload, paramType);
        } catch (JsonProcessingException e) {
            throw new InvalidMessageFormatException(format("invalid json payload for route [%s]", route));
        }

    }

}
