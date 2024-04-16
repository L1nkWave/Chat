package org.linkwave.ws.unit;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.broadcast.SimpleBroadcastManager;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.linkwave.ws.unit.SessionTestUtils.generateSessionIds;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.*;

@ExtendWith(MockitoExtension.class)
public class SimpleBroadcastManagerTest {

    private static final long SESSIONS_COUNT = 10;

    @Mock
    private ChatRepository<Long, String> chatRepository;

    @Mock
    private WebSocketMessageBroadcast messageBroadcast;

    @InjectMocks
    private SimpleBroadcastManager broadcastManager;

    @BeforeEach
    void setUp() {
        // inject fields
        final Field instances = findField(SimpleBroadcastManager.class, "instances");
        if (instances != null) {
            makeAccessible(instances);
            setField(instances, broadcastManager, "E2");
        }

        final Field separator = findField(SimpleBroadcastManager.class, "separator");
        if (separator != null) {
            makeAccessible(separator);
            setField(separator, broadcastManager, ",");
        }
    }

    @SneakyThrows
    @Test
    @DisplayName("Should broadcast message successfully")
    void shouldBroadcastMessageSuccessfully() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "send");
        final var pathVariables = Map.of("id", "1378");
        final var broadcastKey = "chat:1378";
        final Set<String> sessionIds = generateSessionIds(SESSIONS_COUNT);

        when(chatRepository.getSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(TRUE);

        broadcastManager.process(handler, pathVariables, new Object(), jsonMessage);

        verify(chatRepository, times(1)).getSessions(broadcastKey);
        verify(messageBroadcast, times(1)).share(sessionIds, jsonMessage);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should share message between instances")
    void shouldShareMessageBetweenInstances() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "send");
        final var pathVariables = Map.of("id", "1378");
        final var broadcastKey = "chat:1378";
        final Set<String> sessionIds = generateSessionIds(SESSIONS_COUNT);

        when(chatRepository.getSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(FALSE);

        broadcastManager.process(handler, pathVariables, new Object(), jsonMessage);

        verify(chatRepository, times(1)).getSessions(broadcastKey);
        verify(messageBroadcast, times(1)).share(sessionIds, jsonMessage);
        verify(chatRepository, times(1)).shareWithConsumer(any(), any());
    }

    @SneakyThrows
    @Test
    @DisplayName("Should share message locally")
    void shouldShareMessageLocally() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "sendLocally");
        final var pathVariables = Map.of("id", "1378");
        final var broadcastKey = "chat:1378";
        final Set<String> sessionIds = generateSessionIds(SESSIONS_COUNT);

        when(chatRepository.getSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(FALSE);

        broadcastManager.process(handler, pathVariables, new Object(), jsonMessage);

        verify(chatRepository, times(1)).getSessions(broadcastKey);
        verify(messageBroadcast, times(1)).share(sessionIds, jsonMessage);
        verify(chatRepository, times(0)).shareWithConsumer(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when broadcast key is not resolved")
    void shouldThrowExceptionWhenBroadcastKeyIsNotResolved() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "send");
        final var pathVariables = Map.of("ID", "1378");

        assertThrows(IllegalStateException.class, () -> broadcastManager.process(handler, pathVariables, new Object(), jsonMessage));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should resolve broadcast key with message analyze enabled")
    void shouldResolveBroadcastKeyWithMessageAnalyzeEnabled() {
        final var message = new Message(UUID.randomUUID().toString());
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "sendWithMessageAnalysis");
        final var broadcastKey = "chat:%s".formatted(message.chatId);
        final Set<String> sessionIds = generateSessionIds(SESSIONS_COUNT);

        when(chatRepository.getSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(TRUE);

        broadcastManager.process(handler, emptyMap(), message, jsonMessage);

        verify(chatRepository, times(1)).getSessions(broadcastKey);
        verify(messageBroadcast, times(1)).share(sessionIds, jsonMessage);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not broadcast message if annotation not present")
    void shouldNotBroadcastMessageIfAnnotationNotPresent() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "sendWithoutBroadcast");

        broadcastManager.process(handler, emptyMap(), new Object(), jsonMessage);

        verify(chatRepository, never()).getChatMembersSessions(any(String.class));
        verify(messageBroadcast, never()).share(any(), any());
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not broadcast message if members not found")
    void shouldNotBroadcastMessageIfMembersNotFound() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "send");
        final var pathVariables = Map.of("id", "1378");
        final var broadcastKey = "chat:1378";

        when(chatRepository.getSessions(broadcastKey)).thenReturn(emptySet());

        broadcastManager.process(handler, pathVariables, new Object(), jsonMessage);

        verify(chatRepository, times(1)).getSessions(broadcastKey);
        verify(messageBroadcast, never()).share(any(), any());
    }

    private static class RouteHandlers {

        @Broadcast
        void send() {
        }

        void sendWithoutBroadcast() {
        }

        @Broadcast(value = "chat:{chatId}", analyzeMessage = true)
        Message sendWithMessageAnalysis() {
            return new Message(UUID.randomUUID().toString());
        }

        @Broadcast(multiInstances = false)
        Message sendLocally() {
            return new Message(UUID.randomUUID().toString());
        }

    }

    @AllArgsConstructor
    private static class Message {

        private String chatId;

    }

}
