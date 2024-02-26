package org.linkwave.ws.websocket.routing.parser;

import org.linkwave.ws.websocket.routing.RoutingMessage;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TextMessageParser implements MessageParser {

    public static final String PATH_KEY = "path";
    public static final String PATH_DELIMITER = "=";

    @Override
    public RoutingMessage parse(@NonNull String raw) throws InvalidMessageFormatException {

        if (raw.isBlank()) {
            throw new InvalidMessageFormatException("Message is undefined");
        }

        final String[] lines = raw.split("\n");
        final String[] pathLine = lines[0].split(PATH_DELIMITER);

        if (pathLine.length != 2 || !pathLine[0].equals(PATH_KEY) || pathLine[1].isBlank()) {
            throw new InvalidMessageFormatException("Message is unavailable to route");
        }

        final String path = pathLine[1].trim();
        final int payloadLength = lines.length - 2;

        // if payload is absent
        if (payloadLength <= 0) {
            return new RoutingMessage(path, null);
        }

        // extract payload without first two lines
        String[] payloadLines = new String[payloadLength];
        System.arraycopy(lines, 2, payloadLines, 0, payloadLength);

        String payload = String.join("\n", payloadLines);
        return new RoutingMessage(path, payload);
    }

}
