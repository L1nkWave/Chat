package com.chat.wsserver.websocket.routing.parser;

import com.chat.wsserver.websocket.routing.RoutingMessage;
import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TextMessageParser implements MessageParser {

    private static final String PATH_KEY = "path";
    private static final String PATH_DELIMITER = "=";

    @Override
    public RoutingMessage parse(String raw) throws InvalidMessageFormatException {
        try {
            String[] lines = raw.split("\n");
            String[] pathLine = lines[0].split(PATH_DELIMITER);

            if (!pathLine[0].equals(PATH_KEY) || pathLine[1].isBlank()) {
                log.error("-> parse(): path is not defined");
                throw new RuntimeException();
            }

            // extract payload without read lines
            String[] payloadLines = new String[lines.length - 2];
            System.arraycopy(lines, 2, payloadLines, 0, payloadLines.length);

            String payload = String.join("\n", payloadLines);
            return new RoutingMessage(pathLine[1].trim(), payload);

        } catch (RuntimeException e) {
            log.error("-> parse(): message is unavailable to route");
            throw new InvalidMessageFormatException("message is unavailable to route");
        }
    }

}
