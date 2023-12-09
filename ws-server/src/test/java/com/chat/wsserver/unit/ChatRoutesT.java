package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.dto.IncomeMessage;
import com.chat.wsserver.websocket.routing.Payload;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@WebSocketRoute("/chat")
public class ChatRoutesT {

    @SubRoute("/{id}/send")
    void sendMessage(@Payload String message,
                     WebSocketSession session,
                     @PathVariable("id") long id) {
        log.info("-> sendMessage(): chatId={}, msg={}, session={}", id, message, session);

    }

    @SubRoute("/ping")
    void ping(WebSocketSession session) {
        log.info("-> ping()");

    }

    @SubRoute("/{id}/message/{messageId}")
    void updateMessage(WebSocketSession session,
                       @PathVariable("id") long id,
                       @PathVariable("messageId") long messageId,
                       @Payload IncomeMessage message) {
        log.info("-> updateMessage(): id={}, messageId={}", id, messageId);

    }

}
