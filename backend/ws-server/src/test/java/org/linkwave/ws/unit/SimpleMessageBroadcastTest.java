package org.linkwave.ws.unit;

import org.linkwave.ws.websocket.routing.broadcast.SimpleMessageBroadcast;
import org.linkwave.ws.websocket.session.SessionManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import static org.linkwave.ws.unit.SessionTestUtils.generateSessionMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimpleMessageBroadcastTest {

    private static final long SESSIONS_COUNT = 10;

    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private SimpleMessageBroadcast messageBroadcast;

    @SneakyThrows
    @Test
    @DisplayName("Message should be shared completely")
    void messageShouldBeSharedCompletely() {
        final String jsonMessage = "";
        final Map<String, WebSocketSession> sessionMap = generateSessionMap(SESSIONS_COUNT);
        sessionMap.forEach((ssId, session) -> when(sessionManager.find(ssId)).thenReturn(session));

        final boolean isSharedCompletely = messageBroadcast.share(sessionMap.keySet(), jsonMessage);

        assertThat(isSharedCompletely).isTrue();
        sessionMap.values().forEach(session -> {
            try {
                verify(session, times(1)).sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    @Test
    @DisplayName("Message should not be shared completely")
    void messageShouldNotBeSharedCompletely() {
        final String jsonMessage = "";
        final Map<String, WebSocketSession> sessionMap = generateSessionMap(SESSIONS_COUNT);
        final Map<String, WebSocketSession> filteredSessionMap = sessionMap.entrySet()
                .stream()
                .limit(new Random().nextLong(1, sessionMap.size()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        filteredSessionMap.forEach((ssId, session) -> when(sessionManager.find(ssId)).thenReturn(session));

        final boolean isSharedCompletely = messageBroadcast.share(sessionMap.keySet(), jsonMessage);

        assertThat(isSharedCompletely).isFalse();

        sessionMap.entrySet().stream()
                .dropWhile(entry -> filteredSessionMap.containsKey(entry.getKey()))
                .forEach(entry -> {
                    try {
                        verify(entry.getValue(), never()).sendMessage(any());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @SneakyThrows
    @Test
    @DisplayName("Should throw exception when message sending failed")
    void shouldThrowExceptionWhenMessageSendingFailed() {
        final Map<String, WebSocketSession> sessionMap = generateSessionMap(SESSIONS_COUNT);

        for (Entry<String, WebSocketSession> entry : sessionMap.entrySet()) {
            var session = entry.getValue();
            when(sessionManager.find(entry.getKey())).thenReturn(session);
            when(session.getId()).thenReturn(entry.getKey());
            doThrow(IOException.class).when(session).sendMessage(any());
        }

        assertThrows(IOException.class, () -> messageBroadcast.share(sessionMap.keySet(), ""));
    }

}
