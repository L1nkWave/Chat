package org.linkwave.ws.websocket.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.chat.ApiErrorException;
import org.linkwave.ws.api.chat.ChatServiceClient;
import org.linkwave.ws.api.chat.MessageDto;
import org.linkwave.ws.api.chat.NewTextMessage;
import org.linkwave.ws.websocket.dto.*;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.Box;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.linkwave.shared.utils.Bearers.append;
import static org.linkwave.ws.websocket.routing.Box.error;
import static org.linkwave.ws.websocket.routing.Box.ok;

@Slf4j
@WebSocketRoute("/chat")
@RequiredArgsConstructor
public class ChatRoutes {

    private final ChatRepository<Long, String> chatRepository;
    private final ChatServiceClient chatClient;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @SubRoute("/{id}/send")
    @Broadcast("chat:{id}")
    public Box<OutcomeMessage> sendMessage(@PathVariable String id,
                                           @Payload IncomeMessage message,
                                           @NonNull WebSocketSession session,
                                           @NonNull UserPrincipal principal,
                                           @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> sendMessage(): chatId={}, userId={}, msg={}", id, userId, message);

        if (!chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        final MessageDto messageDto;
        try {
            final var newTextMessage = new NewTextMessage(message.text());
            messageDto = chatClient.saveTextMessage(append(principal.rawAccessToken()), id, newTextMessage);

            // send a bind message to initiator
            final var bindMessage = new BindMessage(id, message.tmpMessageId(), messageDto.getId());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(bindMessage)));

        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // build outcome message
        return ok(OutcomeMessage.builder()
                .action(Action.MESSAGE)
                .messageId(messageDto.getId())
                .chatId(id)
                .senderId(userId)
                .text(message.text())
                .timestamp(messageDto.getCreatedAt())
                .build());
    }


    @SubRoute(value = "/{id}/{recipientId}/add", disabled = true)
    public Box<Void> addChat(@PathVariable String id,
                             @PathVariable Long recipientId,
                             @NonNull UserPrincipal principal,
                             @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> addChat(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId) &&
            chatRepository.isMember(id, recipientId)) {
            return ok(null);
        }

        try {
            chatClient.isMember(append(principal.rawAccessToken()), id, recipientId);

            chatRepository.addMember(userId, id);
            chatRepository.addMember(recipientId, id);
            log.debug("-> addChat(): chat graph updated");

            return ok(null);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create("Membership is not confirmed", path));
        }

    }

}
