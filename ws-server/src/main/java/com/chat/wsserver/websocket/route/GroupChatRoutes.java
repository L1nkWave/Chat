package com.chat.wsserver.websocket.route;

import com.chat.wsserver.websocket.dto.OutcomeMessage;
import com.chat.wsserver.websocket.jwt.UserPrincipal;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.Box;
import com.chat.wsserver.websocket.routing.Payload;
import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

import static com.chat.wsserver.websocket.dto.Action.*;
import static com.chat.wsserver.websocket.dto.ErrorMessage.create;
import static com.chat.wsserver.websocket.routing.Box.error;
import static com.chat.wsserver.websocket.routing.Box.ok;

@Slf4j
@WebSocketRoute("/group")
@RequiredArgsConstructor
public class GroupChatRoutes {

    private final ChatRepository<Long> chatRepository;

    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> sendMessage(@PathVariable long id,
                                    @NonNull WebSocketSession session,
                                    @Payload String message) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.debug("-> sendMessage(): chatId={}, userId={}, msg={}", id, userId, message);

        if (!chatRepository.isMember(id, userId)) {
            return error(create("You are not member of chat"));
        }

        // build outcome message
        return ok(OutcomeMessage.builder()
                .action(MESSAGE)
                .chatId(id)
                .senderId(userId)
                .text(message)
                .build());
    }

    @SubRoute("/{id}/join")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> join(@PathVariable long id, @NonNull WebSocketSession session) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.info("-> join(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId)) {
            return error(create("You are already a member of chat"));
        }

        chatRepository.addMember(userId, id);

        return ok(OutcomeMessage.builder()
                .action(JOIN)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @SneakyThrows
    @SubRoute("/{id}/leave")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> leaveChat(@PathVariable long id, @NonNull WebSocketSession session) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.info("-> leaveChat(): chatId={}, userId={}", id, userId);

        if (!chatRepository.isMember(id, userId)) {
            return error(create("You are not member of chat"));
        }

        chatRepository.removeMember(userId, id);

        return ok(OutcomeMessage.builder()
                .action(LEAVE)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @SubRoute("/{id}/message/{messageId}")
    void updateMessage(@PathVariable long id,
                       @PathVariable long messageId,
                       @NonNull WebSocketSession session) {

        log.info("-> updateMessage(): id={}, messageId={}", id, messageId);
        // example route handler
    }

}
