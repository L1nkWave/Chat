package org.linkwave.ws.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.IncomeMessage;
import org.linkwave.ws.websocket.dto.OutcomeMessage;
import org.linkwave.ws.websocket.routing.*;
import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.parser.MessageParser;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

@ExtendWith(MockitoExtension.class)
public class WebSocketRouterImplTest {

    private static final String MESSAGE_TEMPLATE = """
            path=%s
                            
            %s
            """;

    @Mock
    private RouteHandlerArgumentResolver argumentResolver;

    @Mock
    private BroadcastManager broadcastManager;

    @Mock
    private MessageParser messageParser;

    @Mock
    private WebSocketSession session;

    private ObjectMapper objectMapper;
    private WebSocketRouter router;
    private Map<String, RouteComponent> routes;

    @SneakyThrows
    @BeforeEach
    void buildRouter() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        final var wsRouter = new WebSocketRouterImpl(
                messageParser,
                argumentResolver,
                objectMapper,
                broadcastManager
        );

        final var chatRoutes = new ChatRoutesT();
        final Class<?> cls = chatRoutes.getClass();
        final Method routeHandler1 = cls.getDeclaredMethod("sendMessage", String.class, WebSocketSession.class, String.class);
        final Method routeHandler2 = cls.getDeclaredMethod("updateMessage", String.class, String.class, WebSocketSession.class, IncomeMessage.class);

        makeAccessible(routeHandler1);
        makeAccessible(routeHandler2);

        routes = new HashMap<>();

        // setup routes & handlers
        routes.put("/chat/{id}/send", new RouteComponent(chatRoutes, routeHandler1));
        routes.put("/chat/{id}/update_message/{messageId}", new RouteComponent(chatRoutes, routeHandler2));

        // bind routes to router
        final Field routesField = wsRouter.getClass().getDeclaredField("routes");
        makeAccessible(routesField);
        setField(routesField, wsRouter, routes);
        router = wsRouter;
    }

    @SneakyThrows
    @Test
    @DisplayName("Should find route for valid message")
    void shouldFindRouteForValidMessage() {

        final String text = "hello world";

        // prepare 1st message
        final String route = "/chat/{id}/send";
        final String path = "/chat/321/send";
        final String messageWithSimplePayload = format(MESSAGE_TEMPLATE, path, text);
        final var message = new RoutingMessage(path, text);
        final var ctx = new MessageContext(
                entry(route, routes.get(route)),
                Map.of("id", "321"),
                new RoutingMessage(path, text),
                session
        );

        // prepare 2nd message
        final String route2 = "/chat/{id}/update_message/{messageId}";
        final String path2 = "/chat/28712/update_message/831942";
        final String payload2 = objectMapper.writeValueAsString(new IncomeMessage("", text));
        final String messageWithJsonPayload = format(MESSAGE_TEMPLATE, path2, payload2);
        final RoutingMessage message2 = new RoutingMessage(path2, payload2);
        final var ctx2 = new MessageContext(
                entry(route2, routes.get(route2)),
                Map.of("id", "28712", "messageId", "831942"),
                message2, session
        );

        // route 1st message
        when(messageParser.parse(messageWithSimplePayload)).thenReturn(message);
        when(argumentResolver.resolve(ctx)).thenReturn(List.of("321", session, text));

        assertDoesNotThrow(() -> router.route(messageWithSimplePayload, session));

        // route 2nd message
        when(messageParser.parse(messageWithJsonPayload)).thenReturn(message2);
        when(argumentResolver.resolve(ctx2)).thenReturn(List.of("28712", "831942", session, new IncomeMessage("", text)));

        assertDoesNotThrow(() -> router.route(messageWithJsonPayload, session));

        verify(messageParser, times(2)).parse(any());
        verify(argumentResolver, times(2)).resolve(any(MessageContext.class));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("invalidPaths")
    @DisplayName("Should throw exception when router cannot match path with any route")
    void shouldThrowExWhenRouterCannotMatchPathWithAnyRoute(String invalidPath) {
        final String payload = "hello world!";
        final String message = MESSAGE_TEMPLATE.formatted(invalidPath, payload);

        when(messageParser.parse(message)).thenReturn(new RoutingMessage(invalidPath, payload));

        assertThrows(InvalidPathException.class, () -> router.route(message, null));
        verify(messageParser, times(1)).parse(any());
    }

    @SneakyThrows
    @Test
    @DisplayName("Should throw exception for message with empty payload")
    void shouldThrowExForMessageWithEmptyPayload() {
        final String route = "/chat/{id}/send";
        final String path = "/chat/77/send";
        final String message = "path=%s".formatted(path);
        final Entry<String, RouteComponent> matchedRoute = entry(route, routes.get(route));
        final var pathVariables = Map.of("id", "77");

        final var routingMessage = new RoutingMessage(path, null);
        when(messageParser.parse(message)).thenReturn(routingMessage);

        final var context = new MessageContext(matchedRoute, pathVariables, routingMessage, session);
        when(argumentResolver.resolve(context)).thenThrow(InvalidMessageFormatException.class);

        assertThrows(InvalidMessageFormatException.class, () -> router.route(message, session));

        verify(messageParser, times(1)).parse(message);
        verify(argumentResolver, times(1)).resolve(context);
    }

    @NonNull
    static Stream<String> invalidPaths() {
        return Stream.of(
                "/group/321#send",
                "/group/321//send",
                "/group//321//send",
                "/2jd02",
                "///",
                "/group/321",
                "/host",
                "/group"
        );
    }

    @Slf4j
    @WebSocketRoute("/chat")
    private static class ChatRoutesT {

        @Endpoint("/{id}/send")
        Box<OutcomeMessage> sendMessage(@PathVariable String id,
                                        WebSocketSession session,
                                        @Payload String message) {
            return Box.ok(OutcomeMessage.builder()
                    .chatId(id)
                    .action(Action.MESSAGE)
                    .text(message)
                    .build());
        }

        @Endpoint("/{id}/update_message/{messageId}")
        Box<String> updateMessage(@PathVariable String id,
                                  @PathVariable String messageId,
                                  WebSocketSession session,
                                  @Payload IncomeMessage message) {
            return Box.error("just for testing");
        }

    }

}
