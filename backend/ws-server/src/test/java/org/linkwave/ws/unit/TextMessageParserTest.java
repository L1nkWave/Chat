package org.linkwave.ws.unit;

import org.linkwave.ws.websocket.routing.RoutingMessage;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.parser.TextMessageParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.NonNull;

import java.util.stream.Stream;

import static org.linkwave.ws.websocket.routing.parser.TextMessageParser.PATH_DELIMITER;
import static org.linkwave.ws.websocket.routing.parser.TextMessageParser.PATH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextMessageParserTest {

    private final TextMessageParser messageParser = new TextMessageParser();

    private static final String MSG_PATH_TEMPLATE = "%s%s%s";
    private static final String MSG_PATH_PAYLOAD_TEMPLATE = """
            %s%s%s
                                
            %s
            """;


    @Test
    @DisplayName("Message should be successfully parsed" )
    void messageShouldBeSuccessfullyParsed() throws InvalidMessageFormatException {
        final String path = "/group/777/send";
        final String payload = "hello world!";
        final String rawMessage = MSG_PATH_PAYLOAD_TEMPLATE.formatted(PATH_KEY, PATH_DELIMITER, path, payload);
        final var routingMessage = new RoutingMessage(path, payload);

        final var actualRoutingMessage = messageParser.parse(rawMessage);

        assertThat(actualRoutingMessage).isEqualTo(routingMessage);
    }

    @Test
    @DisplayName("Message should be successfully parsed with empty payload" )
    void messageShouldBeSuccessfullyParsedWithEmptyPayload() throws InvalidMessageFormatException {
        final String path = "/group/777/send";
        final String rawMessage = MSG_PATH_TEMPLATE.formatted(PATH_KEY, PATH_DELIMITER, path);
        final var routingMessage = new RoutingMessage(path, null);

        final var actualRoutingMessage = messageParser.parse(rawMessage);

        assertThat(actualRoutingMessage).isEqualTo(routingMessage);
    }

    @ParameterizedTest
    @MethodSource("messagesWithInvalidPaths" )
    @DisplayName("Should throw exception when message path is invalid" )
    void shouldThrowExceptionWhenMessagePathIsInvalid(final String message) {
        assertThrows(InvalidMessageFormatException.class, () -> messageParser.parse(message));
    }

    @NonNull
    static Stream<String> messagesWithInvalidPaths() {
        return Stream.of(
                "Path=/group/777/send",
                "=/group/777/send",
                "path==/group/777/send",
                "path=/group=/777/send",
                "path=",
                "",
                "="
        );
    }

}
