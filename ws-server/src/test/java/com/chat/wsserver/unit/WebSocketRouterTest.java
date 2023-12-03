package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.routing.exception.InvalidPathException;
import com.chat.wsserver.websocket.routing.parser.TextMessageParser;
import com.chat.wsserver.websocket.routing.RouteComponent;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import com.chat.wsserver.websocket.routing.WebSocketRouterImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketRouterTest {

    private static WebSocketRouter router;

    @SneakyThrows
    @BeforeAll
    static void buildRouter() {
        var objectMapper = new ObjectMapper();
        var wsRouter = new WebSocketRouterImpl(new TextMessageParser(), objectMapper, null);

        Map<String, RouteComponent> routes = new HashMap<>();

        ChatRoutesT chatRoutes = new ChatRoutesT();
        Class<?> cls = chatRoutes.getClass();
        Method routeHandler1 = cls.getDeclaredMethod("sendMessage", String.class, WebSocketSession.class, long.class);
        Method routeHandler2 = cls.getDeclaredMethod("updateMessage", WebSocketSession.class, long.class, long.class);

        makeAccessible(routeHandler1);
        makeAccessible(routeHandler2);

        routes.put("/group/{id}/send", new RouteComponent(chatRoutes, routeHandler1));
        routes.put("/group/{id}/update_message/{messageId}", new RouteComponent(chatRoutes, routeHandler2));

        Field routesField = wsRouter.getClass().getDeclaredField("routes");
        makeAccessible(routesField);
        setField(routesField, wsRouter, routes);
        router = wsRouter;
    }

    @Test
    void shouldFindRouteForMessage() {
        final String messageTemplate = """
                path=%s
                
                hello world!
                """;

        final String message = messageTemplate.formatted("/group/321/send");
        final String message2 = messageTemplate.formatted("/group/28712/update_message/831942");

        assertDoesNotThrow(() -> router.route(message, null));
        assertDoesNotThrow(() -> router.route(message2, null));
    }

    @ParameterizedTest
    @MethodSource("invalidPaths")
    void shouldThrowExWhenRouterCannotMatchPathWithAnyRoute(String invalidPath) {
        final String message = """
                path=%s
                
                hello world!
                """.formatted(invalidPath);

        assertThrows(InvalidPathException.class, () -> router.route(message, null));
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

}
