package com.chat.wsserver.websocket.routing.broadcast;

import com.chat.wsserver.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleMessageBroadcast implements WebSocketMessageBroadcast {

    private final SessionManager sessionManager;

    @Override
    public boolean share(@NonNull Set<String> sessionIds, String json) throws IOException {
        int foundSessions = 0, sentFailed = 0;

        for (String ssId : sessionIds) {

            final WebSocketSession session = sessionManager.find(ssId);
            if (session == null) {
                continue;
            }

            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("-> share(): can't send message to ss={}, ssOpen={}", session.getId(), session.isOpen());
                sentFailed++;
            }

            foundSessions++;
        }

        if (sentFailed != 0) {
            throw new IOException("Message sending failed to %d sessions".formatted(sentFailed));
        }

        return foundSessions == sessionIds.size();
    }

}
