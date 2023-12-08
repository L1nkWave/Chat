package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.dto.Action;
import com.chat.wsserver.websocket.dto.OutcomeMessage;
import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

@WebSocketRoute("/group")
public class ChatRoutesBroadcastT {

    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    OutcomeMessage sendMessage(@PathVariable("id") long id,
                               WebSocketSession session, String message) {
        return OutcomeMessage.builder()
                .action(Action.MESSAGE)
                .chatId(id)
                .sender(session.getId())
                .text(message)
                .build();
    }

}
