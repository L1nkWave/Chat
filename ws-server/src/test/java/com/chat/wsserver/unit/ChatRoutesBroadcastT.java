package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.dto.Action;
import com.chat.wsserver.websocket.dto.OutcomeMessage;
import com.chat.wsserver.websocket.jwt.UserPrincipal;
import com.chat.wsserver.websocket.routing.Box;
import com.chat.wsserver.websocket.routing.Payload;
import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

@WebSocketRoute("/group-chat")
public class ChatRoutesBroadcastT {

    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> sendMessage(@PathVariable long id,
                                    @NonNull WebSocketSession session,
                                    @Payload String message) {
        final var principal = (UserPrincipal) session.getPrincipal();
        assert principal != null;
        return Box.ok(OutcomeMessage.builder()
                .action(Action.MESSAGE)
                .chatId(id)
                .senderId(principal.token().userId())
                .text(message)
                .build());
    }

}
