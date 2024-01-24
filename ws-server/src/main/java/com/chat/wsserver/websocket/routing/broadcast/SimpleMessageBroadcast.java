package com.chat.wsserver.websocket.routing.broadcast;

import com.chat.wsserver.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SimpleMessageBroadcast implements WebSocketMessageBroadcast {

    private final SessionManager sessionManager;

    @Override
    public boolean share(@NonNull Set<String> sessionIds, String json) {
        int foundSessions = 0;

        for (String ssId : sessionIds) {

            WebSocketSession session = sessionManager.find(ssId);
            if (session == null) {
                continue;
            }

            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            foundSessions++;
        }

        return foundSessions == sessionIds.size();
    }

}
