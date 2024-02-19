package org.linkwave.ws.websocket;

import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.routing.WebSocketRouter;
import org.linkwave.ws.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RootWebSocketHandler extends AbstractWebSocketHandler {

    private final WebSocketSessionConfigurer sessionConfigurer;
    private final WebSocketRouter router;
    private final SessionManager sessionManager;

    @SneakyThrows
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessionManager.persist(sessionConfigurer.configure(session));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws IOException {

        session = sessionConfigurer.configure(session);

        log.debug("-> handleTextMessage()");
        try {
            router.route(message.getPayload(), session);
        } catch (InvalidMessageFormatException | InvalidPathException e) {
            log.error("-> handleTextMessage(): {}", e.getMessage());
            session.sendMessage(new TextMessage("error: %s".formatted(e.getMessage())));
        }
    }

    @SneakyThrows
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionManager.remove(sessionConfigurer.configure(session));
    }

}
