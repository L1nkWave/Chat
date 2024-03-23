package org.linkwave.ws.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.RouteComponent;
import org.linkwave.ws.websocket.routing.RoutingAutoConfig;
import org.linkwave.ws.websocket.routing.RoutingMessage;
import org.linkwave.ws.websocket.routing.args.DefaultRouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.exception.RoutingException;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.linkwave.ws.unit.SessionTestUtils.createSession;
import static org.mockito.Mockito.mock;

public class DefaultRouteHandlerArgumentResolverTest {

    private RouteHandlerArgumentResolver argumentResolver;

    @BeforeEach
    void setUp() {
        final var argResolverStrategies = new RoutingAutoConfig().argumentResolverStrategies(new ObjectMapper());
        argumentResolver = new DefaultRouteHandlerArgumentResolver(argResolverStrategies);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve non-parametrized route handler")
    public void shouldResolveNonParametrizedRouteHandler() {
        final String route = "/send";
        final var handler = findRouteHandler(route, "send");
        final var session = mock(WebSocketSession.class);

        final List<Object> actualArguments = argumentResolver.resolve(
                handler, emptyMap(),
                new RoutingMessage(route, null),
                session
        );

        assertThat(actualArguments).isEmpty();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve handler arguments successfully")
    void shouldResolveHandlerArgumentsSuccessfully() {
        final String route = "/chat/{id}/send";
        final String path = "/chat/222/send";
        final Map<String, String> pathVariables = Map.of("id", "222");

        final var handler = findRouteHandler(route, "send");
        final var session = mock(WebSocketSession.class);
        final var routingMessage = new RoutingMessage(path, "Hello world!");
        final List<Object> expectedArguments = asList(new Object[]{
                session, routingMessage.payload(),
                parseLong(pathVariables.get("id")),
                0 // int value by default
        });

        final List<Object> actualArguments = argumentResolver.resolve(handler, pathVariables, routingMessage, session);

        assertThat(actualArguments).hasSize(expectedArguments.size());
        assertThat(actualArguments).isEqualTo(expectedArguments);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve path handler argument")
    void shouldResolvePathHandlerArgument() {
        final String route = "/echo", path = "/echo";
        final var handler = findRouteHandler(route, "echoPath");
        final var routingMessage = new RoutingMessage(path, null);

        final List<Object> expectedArguments = asList(new Object[]{path});

        final List<Object> actualArguments = argumentResolver.resolve(
                handler, emptyMap(), routingMessage, createSession().getSecond()
        );

        assertThat(actualArguments).hasSize(expectedArguments.size());
        assertThat(actualArguments).isEqualTo(expectedArguments);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve principal handler argument")
    void shouldResolvePrincipalHandlerArgument() {
        final String route = "/echo", path = "/echo";
        final var handler = findRouteHandler(route, "echoPrincipal");
        final var routingMessage = new RoutingMessage(path, null);
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession();
        final var principal = sessionPair.getFirst();

        final List<Object> expectedArguments = asList(new Object[]{principal, principal});

        final List<Object> actualArguments = argumentResolver.resolve(
                handler, emptyMap(), routingMessage, sessionPair.getSecond()
        );

        assertThat(actualArguments).hasSize(expectedArguments.size());
        assertThat(actualArguments).isEqualTo(expectedArguments);
    }

    @Test
    @DisplayName("Should throw exception when payload is invalid")
    void shouldThrowExceptionWhenPayloadIsInvalid() {
        final var route = "/chat/{id}/send";
        final var path = "/chat/222/send";
        final var pathVariables = Map.of("id", "222");

        final var handlerWithStringPayload = findRouteHandler(route, "send");
        final var handlerWithIntPayload = findRouteHandler(route, "send2");
        final var session = mock(WebSocketSession.class);

        final var message = new RoutingMessage(path, null);
        final var messageWithInvalidPayload = new RoutingMessage(path, "invalid payload");

        assertThrows(InvalidMessageFormatException.class,
                () -> argumentResolver.resolve(
                        handlerWithStringPayload, pathVariables,
                        message, session
                )
        );

        assertThrows(InvalidMessageFormatException.class,
                () -> argumentResolver.resolve(
                        handlerWithIntPayload, pathVariables,
                        messageWithInvalidPayload, session
                )
        );
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve path variables with supported types")
    void shouldResolvePathVariablesWithSupportedTypes() {
        final var route = "/chat/{intId}/send/{stringId}";
        final var path = "/chat/222/send/1d5f";
        final var pathVariables = Map.of("intId", "222", "stringId", "1d5f");

        final var session = mock(WebSocketSession.class);
        final var handler = findRouteHandler(route, "send");
        final var message = new RoutingMessage(path, null);
        final List<Object> expectedArguments = asList(new Object[]{222, "1d5f"});

        final List<Object> actualArguments = argumentResolver.resolve(handler, pathVariables, message, session);

        assertThat(actualArguments).hasSize(expectedArguments.size());
        assertThat(actualArguments).isEqualTo(expectedArguments);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should throw when path variable can't be resolved")
    void shouldThrowExceptionWhenPathVariableCantBeResolved() {
        final var route = "/chat/{ID}/send";
        final var route2 = "/chat/{id}/send";
        final var route3 = "/chat/{objId}/send";
        final var path = "/chat/222/send";

        final var session = mock(WebSocketSession.class);
        final var handler = findRouteHandler(route, "send");
        final var handler2 = findRouteHandler(route2, "send2");
        final var handler3 = findRouteHandler(route3, "send");

        final var message = new RoutingMessage(path, "111");

        // should not find path variable by name
        assertThrows(RoutingException.class,
                () -> argumentResolver.resolve(handler, Map.of("ID", "222"), message, session)
        );

        // incorrect type: string -/> long
        assertThrows(InvalidPathException.class,
                () -> argumentResolver.resolve(handler2, Map.of("id", "string"), message, session)
        );

        // unsupported path variable type
        assertThrows(RoutingException.class,
                () -> argumentResolver.resolve(handler3, Map.of("objId", ""), message, session)
        );
    }

    private static class RouteHandlers {

        @SubRoute("/send")
        void send() {
        }

        @SubRoute("/echo")
        void echoPath(String path) {

        }

        @SubRoute("/echo")
        void echoPrincipal(UserPrincipal userPrincipal, Principal principal) {

        }

        @SubRoute("/chat/{id}/send")
        void send(WebSocketSession session, @Payload String message, @PathVariable long id, int abc) {
        }

        @SubRoute("/chat/{id}/send")
        void send2(@Payload int message, @PathVariable long id) {
        }

        @SubRoute("/chat/{ID}/send")
        void send(@PathVariable long id) {
        }

        @SubRoute("/chat/{objId}/send")
        void send(@PathVariable Object objId) {
        }

        @SubRoute("/chat/{intId}/send/{stringId}")
        void send(@PathVariable int intId, @PathVariable String stringId) {
        }

    }

    @SneakyThrows
    private Map.Entry<String, RouteComponent> findRouteHandler(final String route, final String handlerName) {
        final var handlers = new RouteHandlers();
        final List<Method> routeHandlers = Arrays.stream(handlers.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals(handlerName))
                .filter(method -> {
                    final var ann = method.getAnnotation(SubRoute.class);
                    return ann != null && ann.value().equals(route);
                })
                .toList();

        if (routeHandlers.size() != 1) {
            throw new IllegalStateException(
                    "Route[\"%s\"] named \"%s\" is absent or duplicates".formatted(route, handlerName)
            );
        }

        return Map.entry(route, new RouteComponent(handlers, routeHandlers.get(0)));
    }

}
