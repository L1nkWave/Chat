package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.dto.IncomeMessage;
import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;
import com.chat.wsserver.websocket.routing.exception.InvalidPathException;
import com.chat.wsserver.websocket.routing.parser.TextMessageParser;
import com.chat.wsserver.websocket.routing.RouteComponent;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import com.chat.wsserver.websocket.routing.WebSocketRouterImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketRouterTest {

    private static WebSocketRouter router;
    private static ObjectMapper objectMapper;

    @SneakyThrows
    @BeforeAll
    static void buildRouter() {
        objectMapper = new ObjectMapper();
        var wsRouter = new WebSocketRouterImpl(new TextMessageParser(), objectMapper, null);

        Map<String, RouteComponent> routes = new HashMap<>();

        ChatRoutesT chatRoutes = new ChatRoutesT();
        Class<?> cls = chatRoutes.getClass();
        Method routeHandler1 = cls.getDeclaredMethod("sendMessage", String.class, WebSocketSession.class, long.class);
        Method routeHandler2 = cls.getDeclaredMethod("updateMessage", WebSocketSession.class, long.class, long.class, IncomeMessage.class);
        Method routeHandler3 = cls.getDeclaredMethod("ping", WebSocketSession.class);

        makeAccessible(routeHandler1);
        makeAccessible(routeHandler2);
        makeAccessible(routeHandler3);

        routes.put("/chat/{id}/send", new RouteComponent(chatRoutes, routeHandler1));
        routes.put("/chat/{id}/update_message/{messageId}", new RouteComponent(chatRoutes, routeHandler2));
        routes.put("/chat/ping", new RouteComponent(chatRoutes, routeHandler3));

        // bind routes with router
        Field routesField = wsRouter.getClass().getDeclaredField("routes");
        makeAccessible(routesField);
        setField(routesField, wsRouter, routes);
        router = wsRouter;
    }

    @SneakyThrows
    @Test
    @DisplayName("Should find route for valid message")
    void shouldFindRouteForValidMessage() {
        final String messageTemplate = """
                path=%s
                
                %s
                """;

        final String messageWithSimplePayload = format(messageTemplate, "/chat/321/send", "hello world");
        final String messageWithJsonPayload = format(
                messageTemplate,
                "/chat/28712/update_message/831942",
                objectMapper.writeValueAsString(new IncomeMessage("hello world"))
        );

        assertDoesNotThrow(() -> router.route(messageWithSimplePayload, null));
        assertDoesNotThrow(() -> router.route(messageWithJsonPayload, null));
    }

    @ParameterizedTest
    @MethodSource("invalidPathFormats")
    @DisplayName("Should throw exception for invalid path format")
    void shouldThrowExForInvalidPathFormat(String invalidPathFormat) {
        assertThrows(
                InvalidMessageFormatException.class,
                () -> router.route(invalidPathFormat, null)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPaths")
    @DisplayName("Should throw exception when router cannot match path with any route")
    void shouldThrowExWhenRouterCannotMatchPathWithAnyRoute(String invalidPath) {
        final String message = """
                path=%s
                
                hello world!
                """.formatted(invalidPath);

        assertThrows(InvalidPathException.class, () -> router.route(message, null));
    }

    @Test
    @DisplayName("Should throw exception for message with empty payload")
    void shouldThrowExForMessageWithEmptyPayload() {
        final String message = "path=/chat/77/send";

        assertThrows(InvalidMessageFormatException.class, () -> router.route(message, null));
    }

    @Test
    @DisplayName("Should throw exception for message with invalid payload")
    void shouldThrowExForMessageWithInvalidPayload() {
        final String message = """
                path=/chat/1/update_message/1
                
                hello world!
                """;

        assertThrows(InvalidMessageFormatException.class, () -> router.route(message, null));
    }

    @Test
    @DisplayName("Should find route with empty payload")
    void shouldFindRouteWithEmptyPayload() {
        final String message = "path=/chat/ping";

        assertDoesNotThrow(() -> router.route(message, null));
    }

    static Stream<String> invalidPaths() {
        return Stream.of(
                "/group/321#send",
                "/group/321//send",
                "/group//321//send",
                "/2jd02",
                "///",
                "/group/321",
                "/host",
                "/group.312.send"
        );
    }

    static Stream<String> invalidPathFormats() {
        return Stream.of("path=", "=/chat", "", "=");
    }

}
