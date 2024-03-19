package org.linkwave.ws.websocket.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.chat.ApiErrorException;
import org.linkwave.ws.api.chat.ChatServiceClient;
import org.linkwave.ws.api.chat.GroupChatDto;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.ChatMessage;
import org.linkwave.ws.websocket.dto.ErrorMessage;
import org.linkwave.ws.websocket.dto.client.NewGroupChat;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.Box;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;

import static org.linkwave.shared.utils.Bearers.append;
import static org.linkwave.ws.websocket.routing.Box.*;

@Slf4j
@WebSocketRoute("/chat/group")
@RequiredArgsConstructor
public class GroupChatRoutes {

    private final ChatRepository<Long, String> chatRepository;
    private final ChatServiceClient chatClient;

    @SubRoute(value = "/create")
    public Box<GroupChatDto> createChat(@NonNull UserPrincipal principal,
                                        @Payload NewGroupChat body,
                                        @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> createChat(): userId={}", userId);

        final GroupChatDto groupChat;
        try {
            groupChat = chatClient.createGroupChat(append(principal.rawAccessToken()), body);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // update chats graph
        chatRepository.addMember(userId, groupChat.getId());

        return ok(groupChat);
    }

    @SubRoute("/{id}/join")
    @Broadcast("chat:{id}")
    public Box<ChatMessage> join(@PathVariable String id,
                                 @NonNull UserPrincipal principal,
                                 @NonNull String path) {

        final Long userId = principal.token().userId();
        log.info("-> join(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are already a member of chat", path));
        }

        // api call to add member
        try {
            chatClient.joinGroupChat(append(principal.rawAccessToken()), id);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // update chats graph
        chatRepository.addMember(userId, id);

        return ok(ChatMessage.builder()
                .action(Action.JOIN)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @SubRoute("/{id}/leave")
    @Broadcast("chat:{id}")
    public Box<ChatMessage> leaveChat(@PathVariable String id,
                                      @NonNull UserPrincipal principal,
                                      @NonNull String path) {

        final Long userId = principal.token().userId();
        log.info("-> leaveChat(): chatId={}, userId={}", id, userId);

        if (!chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        // api call to remove member
        try {
            chatClient.leaveGroupChat(append(principal.rawAccessToken()), id);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // update chats graph
        chatRepository.removeMember(userId, id);
        return ok(ChatMessage.builder()
                .action(Action.LEAVE)
                .chatId(id)
                .senderId(userId)
                .build());
    }

}
