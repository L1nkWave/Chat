package org.linkwave.ws.unit;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.ws.websocket.dto.OutcomeMessage;
import org.linkwave.ws.websocket.routing.*;
import org.linkwave.ws.websocket.routing.args.RouteHandlerArgumentResolver;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.exception.ConditionViolatedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConditionalRouteHandlerInvocatorTest {

    @Mock
    private RouteHandlerArgumentResolver argumentResolver;

    @InjectMocks
    private ConditionalRouteHandlerInvocator routeHandlerInvocator;

    @SneakyThrows
    @Test
    void Should_InvokeRouteHandler() {
        final RouteHandlers routeHandlers = new RouteHandlers();
        final Method handler = getRequiredMethod(RouteHandlers.class, "send", String.class);
        makeAccessible(handler);

        final RouteComponent routeComponent = new RouteComponent(routeHandlers, handler, emptyList());
        final String path = "/send";
        final String payload = "Hello world";

        final var ctx = new MessageContext(
                Map.entry(path, routeComponent),
                emptyMap(),
                new RoutingMessage(path, payload),
                mock(WebSocketSession.class)
        );

        final var expectedOutcomeMessage = OutcomeMessage.builder()
                .text(payload)
                .build();

        when(argumentResolver.resolve(ctx)).thenReturn(List.of(payload));

        final Object invocationResult = routeHandlerInvocator.delegateInvocation(ctx);

        assertThat(invocationResult).isNotNull();
        assertThat(invocationResult).isInstanceOf(OutcomeMessage.class);
        assertThat(((OutcomeMessage) invocationResult).getText()).isEqualTo(expectedOutcomeMessage.getText());

        verify(argumentResolver, only()).resolve(ctx);
    }

    @SneakyThrows
    @Test
    void Should_GetIntoRouteHandler_WhenPassThroughRegisteredConditions() {
        final RouteHandlers routeHandlers = new RouteHandlers();
        final Method handler = getRequiredMethod(RouteHandlers.class, "conditionalSend", String.class);
        makeAccessible(handler);

        final RouteComponent routeComponent = new RouteComponent(routeHandlers, handler, emptyList());
        final String path = "/send";
        final String payload = "Hello world";

        final var ctx = new MessageContext(
                Map.entry(path, routeComponent),
                Map.of("id", "123"),
                new RoutingMessage(path, payload),
                mock(WebSocketSession.class)
        );

        when(argumentResolver.resolve(ctx)).thenReturn(List.of(payload));

        routeHandlerInvocator.delegateInvocation(ctx);

        verify(argumentResolver, only()).resolve(ctx);
    }

    @SneakyThrows
    @Test
    void Should_ThrowException_WhenConditionsAreViolated() {
        final RouteHandlers routeHandlers = new RouteHandlers();
        final Method handler = getRequiredMethod(RouteHandlers.class, "conditionalSend", String.class);
        makeAccessible(handler);

        final List<EndpointCondition> conditions = List.of(new FirstCondition(), new SecondCondition());
        final RouteComponent routeComponent = new RouteComponent(routeHandlers, handler, conditions);
        final String path = "/send";
        final String payload = "Hello world";

        final var ctx = new MessageContext(
                Map.entry(path, routeComponent),
                emptyMap(),
                new RoutingMessage(path, payload),
                mock(WebSocketSession.class)
        );

        assertThrows(ConditionViolatedException.class, () -> routeHandlerInvocator.delegateInvocation(ctx));
    }

    static class RouteHandlers {

        @Endpoint("/send")
        public OutcomeMessage send(@Payload String payload) {
            return OutcomeMessage.builder()
                    .text(payload)
                    .build();
        }

        @Endpoint(
                value = "/send",
                conditions = {
                        FirstCondition.class,
                        SecondCondition.class
                }
        )
        public OutcomeMessage conditionalSend(@Payload String payload) {
            return OutcomeMessage.builder()
                    .text(payload)
                    .build();
        }

    }

    static class FirstCondition implements EndpointCondition {

        @Override
        public void check(@NonNull MessageContext context) {

        }
    }

    static class SecondCondition implements EndpointCondition {

        @Override
        public void check(@NonNull MessageContext context) {
            if (context.pathVariables().isEmpty()) {
                throw new ConditionViolatedException("Path variables are empty");
            }
        }
    }

}
