package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.dto.IncomeMessage;
import com.chat.wsserver.websocket.routing.Payload;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@WebSocketRoute("/chat")
public class ChatRoutesT {

    @SubRoute("/{id}/send")
    void sendMessage(@PathVariable long id,
                     @NonNull WebSocketSession session,
                     @Payload String message) {
        log.debug("-> sendMessage(): chatId={}, msg={}, session={}", id, message, session);
    }

    @SubRoute("/ping")
    void ping(WebSocketSession session) {
        log.debug("-> ping()");
    }

    @SubRoute("/{id}/update_message/{messageId}")
    void updateMessage(@PathVariable long id,
                       @PathVariable long messageId,
                       @NonNull WebSocketSession session,
                       @Payload IncomeMessage message) {
        log.debug("-> updateMessage(): id={}, messageId={}", id, messageId);
    }

}
