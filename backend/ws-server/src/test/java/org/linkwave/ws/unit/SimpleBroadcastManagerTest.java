package org.linkwave.ws.unit;

import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.broadcast.SimpleBroadcastManager;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.linkwave.ws.unit.SessionTestUtils.generateSessionIds;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.*;

@ExtendWith(MockitoExtension.class)
public class SimpleBroadcastManagerTest {

    private static final long SESSIONS_COUNT = 10;

    @Mock
    private ChatRepository<Long> chatRepository;

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

        when(chatRepository.getChatMembersSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(TRUE);

        broadcastManager.process(handler, pathVariables, jsonMessage);

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

        when(chatRepository.getChatMembersSessions(broadcastKey)).thenReturn(sessionIds);
        when(messageBroadcast.share(sessionIds, jsonMessage)).thenReturn(FALSE);

        broadcastManager.process(handler, pathVariables, jsonMessage);

        verify(messageBroadcast, times(1)).share(sessionIds, jsonMessage);
        verify(chatRepository, times(1)).shareWithConsumer(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when broadcast key is not resolved")
    void shouldThrowExceptionWhenBroadcastKeyIsNotResolved() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "send");
        final var pathVariables = Map.of("ID", "1378");

        assertThrows(IllegalStateException.class, () -> broadcastManager.process(handler, pathVariables, jsonMessage));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not broadcast message if annotation not present")
    void shouldNotBroadcastMessageIfAnnotationNotPresent() {
        final String jsonMessage = "";
        final Method handler = getRequiredMethod(RouteHandlers.class, "sentWithoutBroadcast");

        broadcastManager.process(handler, emptyMap(), jsonMessage);

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

        when(chatRepository.getChatMembersSessions(broadcastKey)).thenReturn(emptySet());

        broadcastManager.process(handler, pathVariables, jsonMessage);

        verify(chatRepository, times(1)).getChatMembersSessions(broadcastKey);
        verify(messageBroadcast, never()).share(any(), any());
    }

    private static class RouteHandlers {

        @Broadcast("chat:{id}")
        void send() {
        }

        void sentWithoutBroadcast() {
        }

    }

}
