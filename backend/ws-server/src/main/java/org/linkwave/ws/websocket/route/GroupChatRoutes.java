package org.linkwave.ws.websocket.route;

import org.linkwave.ws.websocket.dto.OutcomeMessage;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.Box;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.ErrorMessage;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@WebSocketRoute("/group")
@RequiredArgsConstructor
public class GroupChatRoutes {

    private final ChatRepository<Long> chatRepository;

    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> sendMessage(@PathVariable("id") long id,
                                    @NonNull WebSocketSession session,
                                    @Payload String message) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.debug("-> sendMessage(): chatId={}, userId={}, msg={}", id, userId, message);

        if (!chatRepository.isMember(id, userId)) {
            return Box.error(ErrorMessage.create("You are not member of chat"));
        }

        // build outcome message
        return Box.ok(OutcomeMessage.builder()
                .action(Action.MESSAGE)
                .chatId(id)
                .senderId(userId)
                .text(message)
                .build());
    }

    @SubRoute("/{id}/join")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> join(@PathVariable("id") long id, @NonNull WebSocketSession session) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.info("-> join(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId)) {
            return Box.error(ErrorMessage.create("You are already a member of chat"));
        }

        chatRepository.addMember(userId, id);

        return Box.ok(OutcomeMessage.builder()
                .action(Action.JOIN)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @SneakyThrows
    @SubRoute("/{id}/leave")
    @Broadcast("chat:{id}")
    Box<OutcomeMessage> leaveChat(@PathVariable("id") long id, @NonNull WebSocketSession session) {

        final Long userId = ((UserPrincipal) session.getPrincipal()).token().userId();
        log.info("-> leaveChat(): chatId={}, userId={}", id, userId);

        if (!chatRepository.isMember(id, userId)) {
            return Box.error(ErrorMessage.create("You are not member of chat"));
        }

        chatRepository.removeMember(userId, id);

        return Box.ok(OutcomeMessage.builder()
                .action(Action.LEAVE)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @SubRoute("/{id}/message/{messageId}")
    void updateMessage(@PathVariable("id") long id,
                       @PathVariable("messageId") long messageId,
                       @NonNull WebSocketSession session) {

        log.info("-> updateMessage(): id={}, messageId={}", id, messageId);
        // example route handler
    }

}
