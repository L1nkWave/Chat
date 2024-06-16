package org.linkwave.ws.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastRepositoryResolver;
import org.linkwave.ws.websocket.routing.broadcast.FlexBroadcastManager;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.linkwave.ws.unit.SessionTestUtils.generateSessionIds;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlexBroadcastManagerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BroadcastRepositoryResolver repositoryResolver;

    @Mock
    private WebSocketMessageBroadcast messageBroadcast;

    @InjectMocks
    private FlexBroadcastManager broadcastManager;

    @Test
    void Should_DetectRouteHandler_WhenItAnnotatedWithBroadcast() {
        final Method send = getRequiredMethod(Routes.class, "send", String.class);
        final Method sendWithBroadcast = getRequiredMethod(Routes.class, "sendWithBroadcast");

        assertThat(broadcastManager.isBroadcast(send)).isTrue();
        assertThat(broadcastManager.isBroadcast(sendWithBroadcast)).isFalse();
    }

    @Test
    @SneakyThrows
    void Should_PerformMultipleBroadcast_When_RouteHandlerHasSeveralAnnotations() {

        final Method send = getRequiredMethod(Routes.class, "send", String.class);
        final var pathVariables = Map.of("id", "xyz");
        final var message = new Message(pathVariables.get("id"), 1L);
        final String serializedMessage = objectMapper.writeValueAsString(message);

        final Set<String> firstBroadcastSessions = generateSessionIds(5L);
        final Set<String> secondBroadcastSessions = generateSessionIds(10L);

        // sessions for first broadcast
        when(repositoryResolver.resolve("chat:{id}", "chat:%s".formatted(pathVariables.get("id"))))
                .thenReturn(firstBroadcastSessions);

        // sessions for second broadcast
        when(repositoryResolver.resolve("user:{senderId}", "user:%s".formatted(message.senderId())))
                .thenReturn(secondBroadcastSessions);

        when(messageBroadcast.share(anySet(), eq(serializedMessage))).thenReturn(TRUE);

        broadcastManager.process(send, pathVariables, message, serializedMessage);

        verify(messageBroadcast, times(1)).share(firstBroadcastSessions, serializedMessage);
        verify(messageBroadcast, times(1)).share(secondBroadcastSessions, serializedMessage);
    }

    @Test
    @SneakyThrows
    void Should_PerformSingleBroadcast_When_DuplicateKeysDetected() {

        final Method sendDuplicates = getRequiredMethod(Routes.class, "sendDuplicates", String.class);
        final var pathVariables = Map.of("id", "xyz");
        final var message = new Message(pathVariables.get("id"), 1L);
        final String serializedMessage = objectMapper.writeValueAsString(message);

        final Set<String> resolvedSessions = generateSessionIds(5L);

        when(repositoryResolver.resolve("chat:{id}", "chat:%s".formatted(pathVariables.get("id"))))
                .thenReturn(resolvedSessions);

        when(messageBroadcast.share(anySet(), eq(serializedMessage))).thenReturn(TRUE);

        broadcastManager.process(sendDuplicates, pathVariables, message, serializedMessage);

        verify(messageBroadcast, times(1)).share(anySet(), anyString());
    }

    @Test
    @SneakyThrows
    void Should_SkipBroadcast_When_SessionsNotFound() {
        final Method send = getRequiredMethod(Routes.class, "send", String.class);
        final var pathVariables = Map.of("id", "xyz");
        final var message = new Message(pathVariables.get("id"), 1L);
        final String serializedMessage = objectMapper.writeValueAsString(message);

        // sessions for first broadcast
        when(repositoryResolver.resolve("chat:{id}", "chat:%s".formatted(pathVariables.get("id"))))
                .thenReturn(Collections.emptySet());

        // sessions for second broadcast
        when(repositoryResolver.resolve("user:{senderId}", "user:%s".formatted(message.senderId())))
                .thenReturn(Collections.emptySet());

        broadcastManager.process(send, pathVariables, message, serializedMessage);

        verify(messageBroadcast, never()).share(any(), any());
    }

    static class Routes {

        @Broadcast
        @Broadcast(value = "user:{senderId}", analyzeMessage = true)
        @Endpoint("/chat/{id}")
        Message send(@PathVariable String id) {
            return new Message(id, 1L);
        }

        @Broadcast
        @Broadcast
        @Endpoint("/chat/{id}")
        Message sendDuplicates(@PathVariable String id) {
            return new Message(id, 1L);
        }

        void sendWithBroadcast() {

        }

    }

    record Message(String id, Long senderId) {}

}
