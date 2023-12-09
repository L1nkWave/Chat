package com.chat.wsserver.websocket.route;

import com.chat.wsserver.websocket.dto.OutcomeMessage;
import com.chat.wsserver.websocket.routing.Payload;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

import static com.chat.wsserver.websocket.dto.Action.*;

@Slf4j
@WebSocketRoute("/group")
@RequiredArgsConstructor
public class GroupChatRoutes {

    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;

    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    OutcomeMessage sendMessage(@PathVariable("id") long id, WebSocketSession session,
                               @Payload String message) throws IOException {

        String sessionId = session.getId();
        log.info("-> sendMessage(): chatId={}, msg={}, ss={}", id, message, sessionId);

        if (!chatRepository.isMember(id, sessionId)) {
            log.error("-> sendMessage(): chatId={}, ss={}", id, sessionId);

            String errorPayload = objectMapper.writeValueAsString(Map.of(
                    "action", ERROR,
                    "text", "you are not member of chat"
            ));

            session.sendMessage(new TextMessage(errorPayload));
            return null;
        }

        // build outcome message
        return OutcomeMessage.builder()
                .action(MESSAGE)
                .chatId(id)
                .sender(sessionId)
                .text(message)
                .build();
    }

    @SubRoute("/{id}/join")
    @Broadcast("chat:{id}")
    OutcomeMessage join(@PathVariable("id") long id, WebSocketSession session) {
        log.info("-> join(): id={}", id);

        String sessionId = session.getId();
        chatRepository.addMember(id, sessionId);

        return OutcomeMessage.builder()
                .action(JOIN)
                .chatId(id)
                .sender(sessionId)
                .build();
    }

    @SneakyThrows
    @SubRoute("/{id}/leave")
    @Broadcast(value = "chat:{id}")
    OutcomeMessage leaveChat(@PathVariable("id") long id, WebSocketSession session) {

        String sessionId = session.getId();
        log.info("-> leaveChat(): id={}, ss={}", id, sessionId);

        chatRepository.removeMember(id, sessionId);

        return OutcomeMessage.builder()
                .action(LEAVE)
                .chatId(id)
                .sender(sessionId)
                .build();
    }

    @SubRoute("/{id}/message/{messageId}")
    void updateMessage(WebSocketSession session,
                       @PathVariable("id") long id,
                       @PathVariable("messageId") long messageId) {

        log.info("-> updateMessage(): id={}, messageId={}", id, messageId);
        // example route handler
    }

}
