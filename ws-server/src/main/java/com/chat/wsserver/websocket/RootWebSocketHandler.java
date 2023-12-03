package com.chat.wsserver.websocket;

import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;
import com.chat.wsserver.websocket.routing.exception.InvalidPathException;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RootWebSocketHandler extends AbstractWebSocketHandler {

    private final Map<String, WebSocketSession> sessionMap;
    private final WebSocketRouter router;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.info("user[{}] connected", session.getId());
        sessionMap.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws IOException {
        log.info("-> handleTextMessage(): ss={}", session.getId());
        try {
            router.route(message.getPayload(), session);
        } catch (InvalidMessageFormatException | InvalidPathException e) {
            log.error("-> handleTextMessage(): {}", e.getMessage());
            session.sendMessage(new TextMessage("error: %s".formatted(e.getMessage())));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) {
        log.info("user[{}] disconnected", session.getId());
        sessionMap.remove(session.getId());
    }



}
