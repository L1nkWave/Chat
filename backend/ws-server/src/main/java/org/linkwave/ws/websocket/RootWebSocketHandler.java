package org.linkwave.ws.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.dto.ErrorMessage;
import org.linkwave.ws.websocket.routing.WebSocketRouter;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.linkwave.ws.websocket.session.SessionManager;
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
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {

        session = sessionConfigurer.configure(session);

        try {
            sessionManager.persist(session);
        } catch (RuntimeException e) {
            log.error("-> afterConnectionEstablished(): {}", e.getMessage());
            session.sendMessage(createErrorMessage(e.getMessage()));
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws IOException {

        log.debug("-> handleTextMessage()");
        session = sessionManager.find(session.getId());

        // check if session is not expired
        if (session == null) {
            return;
        }

        try {
            router.route(message.getPayload(), session);
        } catch (InvalidMessageFormatException | InvalidPathException e) {
            log.error("-> handleTextMessage(): {}", e.getMessage());
            session.sendMessage(createErrorMessage(e.getMessage()));
        }
    }

    @SneakyThrows
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionManager.remove(sessionConfigurer.configure(session));
    }

    @SneakyThrows
    @NonNull
    private TextMessage createErrorMessage(String message) {
        return new TextMessage(
                objectMapper.writeValueAsString(
                        ErrorMessage.create(message, null)
                )
        );
    }

}
