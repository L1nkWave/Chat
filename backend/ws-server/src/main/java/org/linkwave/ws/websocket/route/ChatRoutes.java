package org.linkwave.ws.websocket.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.ApiErrorException;
import org.linkwave.ws.api.chat.*;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.dto.*;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.Box;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
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
    @Broadcast
    @Endpoint("/{id}/send")
    public Box<OutcomeMessage> sendTextMessage(@PathVariable String id,
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
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        final String messageId = messageDto.getId();

        // add unread message for each chat member
        final Set<Long> members = chatRepository.getMembers(id);
        members.remove(userId);
        chatRepository.changeUnreadMessages(id, members, 1);

        // send a bind message to initiator
        final var bindMessage = new BindMessage(Action.BIND, id, message.tmpMessageId(), messageId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(bindMessage)));

        // build outcome message
        return ok(OutcomeMessage.builder()
                .action(Action.MESSAGE)
                .id(messageId)
                .chatId(id)
                .senderId(userId)
                .text(message.text())
                .timestamp(messageDto.getCreatedAt())
                .build());
    }

    @Endpoint("/edit_text_message/{messageId}")
    @Broadcast(value = "chat:{chatId}", analyzeMessage = true)
    public Box<OutcomeMessage> editTextMessage(@PathVariable String messageId,
                                               @Payload String text,
                                               @NonNull UserPrincipal principal,
                                               @NonNull String path) {

        final UpdatedTextMessage updatedMessage;
        try {
            updatedMessage = chatClient.editTextMessage(
                    append(principal.rawAccessToken()),
                    messageId,
                    new NewTextMessage(text)
            );
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        return ok(OutcomeMessage.builder()
                .action(Action.UPD_MESSAGE)
                .id(updatedMessage.getMessageId())
                .chatId(updatedMessage.getChatId())
                .text(updatedMessage.getText())
                .timestamp(updatedMessage.getEditedAt())
                .senderId(principal.token().userId())
                .build());
    }

    @Broadcast
    @Endpoint("/{id}/send_file")
    public Box<OutcomeFileMessage> sendFileMessage(@PathVariable String id,
                                                   @Payload CreatedFileMessage message,
                                                   @NonNull UserPrincipal principal,
                                                   @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> sendFileMessage(): chatId={}, userId={}", id, userId);

        if (!chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        try {
            chatClient.isOwnFileMessage(
                    append(principal.rawAccessToken()),
                    id,
                    message.getId(),
                    message.getFilename(),
                    message.getContentType(),
                    message.getSize()
            );
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create("Uploaded file message not found", path));
        }

        // unread messages counter has to be already changed in Chat API

        return ok(OutcomeFileMessage.builder()
                .id(message.getId())
                .chatId(id)
                .senderId(userId)
                .timestamp(message.getCreatedAt())
                .filename(message.getFilename())
                .contentType(message.getContentType())
                .size(message.getSize())
                .build());
    }

    @Endpoint("/remove_message/{messageId}")
    @Broadcast(value = "chat:{chatId}", analyzeMessage = true)
    public Box<IdentifiedMessage> removeMessage(@PathVariable String messageId,
                                                @NonNull UserPrincipal principal,
                                                @NonNull String path) {

        final RemovedMessage removedMessage;
        try {
            removedMessage = chatClient.removeMessage(append(principal.rawAccessToken()), messageId);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        final String chatId = removedMessage.getChatId();

        // decrement message counter for those who has not read chat
        // since creation time of the removed message
        final Set<Long> filteredMembers = chatRepository.getLastReadMessages(chatId)
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isBefore(removedMessage.getCreatedAt()))
                .map(Map.Entry::getKey)
                .collect(toSet());

        chatRepository.changeUnreadMessages(chatId, filteredMembers, -1);

        return ok(IdentifiedMessage.builder()
                .action(Action.REMOVE)
                .id(messageId)
                .chatId(chatId)
                .senderId(principal.token().userId())
                .build());
    }

    @Broadcast
    @Endpoint("/{id}/clear_history")
    public Box<ChatMessage> clearChatHistory(@PathVariable String id,
                                             @NonNull UserPrincipal principal,
                                             @NonNull String path) {

        final Long userId = principal.token().userId();
        if (!chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        try {
            chatClient.clearMessages(append(principal.rawAccessToken()), id);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // reset unread messages counter
        chatRepository.setUnreadMessages(id, 0);

        return ok(ChatMessage.builder()
                .action(Action.CLEAR_HISTORY)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @Broadcast
    @Endpoint("/{id}/read")
    public Box<ReadMessage> readMessages(@PathVariable String id,
                                         @Payload LastReadMessage message,
                                         @NonNull UserPrincipal principal,
                                         @NonNull String path) {

        final Long userId = principal.token().userId();

        if (!chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        final ReadMessages readMessages;
        try {
            readMessages = chatClient.readMessages(
                    append(principal.rawAccessToken()),
                    id,
                    message.getTimestamp()
            );
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // subtract unread messages keeping counter gte 0
        final int unreadMessages = chatRepository.getUnreadMessages(id, userId);
        final int minCount = Math.min(unreadMessages, readMessages.getReadCount());
        if (minCount > 0) {
            chatRepository.changeUnreadMessages(
                    id, userId,
                    -minCount,
                    readMessages.getCursor().getTimestamp()
            );
        }

        if (!readMessages.getUnreadMessages().isEmpty()) {
            return ok(ReadMessage.builder()
                    .senderId(userId)
                    .chatId(id)
                    .messages(readMessages.getUnreadMessages())
                    .build());
        }
        return ok();
    }

    @Endpoint(value = "/{id}/{recipientId}/add", disabled = true)
    public Box<Void> addChat(@PathVariable String id,
                             @PathVariable Long recipientId,
                             @NonNull UserPrincipal principal,
                             @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> addChat(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId) &&
            chatRepository.isMember(id, recipientId)) {
            return ok();
        }

        try {
            chatClient.isMember(append(principal.rawAccessToken()), id, recipientId);

            chatRepository.addMember(userId, id);
            chatRepository.addMember(recipientId, id);
            log.debug("-> addChat(): chat graph updated");

            return ok();
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create("Membership is not confirmed", path));
        }

    }

}
