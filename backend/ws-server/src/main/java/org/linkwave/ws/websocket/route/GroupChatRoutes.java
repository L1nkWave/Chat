package org.linkwave.ws.websocket.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.ApiErrorException;
import org.linkwave.ws.api.chat.ChatMemberDto;
import org.linkwave.ws.api.chat.ChatServiceClient;
import org.linkwave.ws.api.chat.GroupChatDto;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.dto.*;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.Box;
import org.linkwave.ws.websocket.routing.Payload;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.linkwave.shared.utils.Bearers.append;
import static org.linkwave.ws.websocket.routing.Box.error;
import static org.linkwave.ws.websocket.routing.Box.ok;

@Slf4j
@WebSocketRoute("/chat/group")
@RequiredArgsConstructor
public class GroupChatRoutes {

    private final ChatRepository<Long, String> chatRepository;
    private final WebSocketMessageBroadcast messageBroadcast;
    private final ChatServiceClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${server.instances.list}")
    private String[] instances;

    @Value("${server.instances.enabled}")
    private boolean isMibEnabled;

    @Endpoint(value = "/create", disabled = true)
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

    @Endpoint(value = "/{id}/add", disabled = true)
    public Box<Void> addChat(@PathVariable String id,
                             @NonNull UserPrincipal principal,
                             @NonNull String path) {

        final Long userId = principal.token().userId();
        log.debug("-> addChat(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId)) {
            return Box.ok(null);
        }

        try {
            chatClient.isGroupChatMember(append(principal.rawAccessToken()), id);
            chatRepository.addMember(userId, id);
            log.debug("-> addChat(): chat graph updated");

            return Box.ok(null);
        } catch (ApiErrorException e) {
            return Box.error(ErrorMessage.create("Membership is not confirmed", path));
        }
    }

    @Broadcast
    @Endpoint("/{id}/join")
    public Box<MemberMessage> join(@PathVariable String id,
                                   @NonNull UserPrincipal principal,
                                   @NonNull String path) {

        final Long userId = principal.token().userId();
        log.info("-> join(): chatId={}, userId={}", id, userId);

        if (chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("You are already a member of chat", path));
        }

        // api call to add member
        final ChatMemberDto newMember;
        try {
            newMember = chatClient.joinGroupChat(append(principal.rawAccessToken()), id);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // add unread message for each chat member
        chatRepository.changeUnreadMessages(id, chatRepository.getMembers(id), 1);

        // update chats graph
        chatRepository.addMember(userId, id);

        return ok(MemberMessage.builder()
                .action(Action.JOIN)
                .chatId(id)
                .senderId(userId)
                .timestamp(newMember.getJoinedAt())
                .memberDetails(newMember.getDetails())
                .build());
    }

    @Broadcast
    @Broadcast(value = "user:{senderId}", analyzeMessage = true)
    @Endpoint("/{id}/leave")
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

        // add unread message for each chat member
        chatRepository.changeUnreadMessages(id, chatRepository.getMembers(id), 1);

        return ok(ChatMessage.builder()
                .action(Action.LEAVE)
                .chatId(id)
                .senderId(userId)
                .build());
    }

    @Broadcast
    @Endpoint("/{id}/add_member/{userId}")
    public Box<MemberMessage> addMember(@PathVariable String id,
                                        @PathVariable Long userId,
                                        @NonNull UserPrincipal principal,
                                        @NonNull String path) {

        final Long initiatorUserId = principal.token().userId();

        if (initiatorUserId.equals(userId)) {
            return error(ErrorMessage.create("Invalid user ID", path));
        }

        if (!chatRepository.isMember(id, initiatorUserId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        if (chatRepository.isMember(id, userId)) {
            return error(ErrorMessage.create("Already a member of chat", path));
        }

        final ChatMemberDto newMember;
        try {
            newMember = chatClient.addGroupChatMember(append(principal.rawAccessToken()), id, userId);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // add unread message for each chat member
        final Set<Long> members = chatRepository.getMembers(id);
        members.remove(initiatorUserId);
        chatRepository.changeUnreadMessages(id, members, 1);

        // update chats graph
        chatRepository.addMember(userId, id);

        return ok(MemberMessage.builder()
                .action(Action.ADD)
                .senderId(initiatorUserId)
                .chatId(id)
                .memberId(userId)
                .memberDetails(newMember.getDetails())
                .timestamp(newMember.getJoinedAt())
                .build());
    }

    @Broadcast
    @Broadcast("user:{memberId}")
    @Endpoint("/{id}/kick/{memberId}")
    public Box<MemberMessage> removeMember(@PathVariable String id,
                                           @PathVariable Long memberId,
                                           @NonNull UserPrincipal principal,
                                           @NonNull String path) {

        final Long initiatorUserId = principal.token().userId();

        if (initiatorUserId.equals(memberId)) {
            return error(ErrorMessage.create("Invalid user ID", path));
        }

        if (!chatRepository.isMember(id, initiatorUserId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        if (!chatRepository.isMember(id, memberId)) {
            return error(ErrorMessage.create("Member not found", path));
        }

        final ChatMemberDto removedMember;
        try {
            removedMember = chatClient.removeGroupChatMember(append(principal.rawAccessToken()), id, memberId);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        // add unread message for each chat member
        final Set<Long> members = chatRepository.getMembers(id);
        members.remove(initiatorUserId);
        chatRepository.changeUnreadMessages(id, members, 1);

        // remove member from redis
        chatRepository.removeMember(memberId, id);

        return ok(MemberMessage.builder()
                .action(Action.KICK)
                .senderId(initiatorUserId)
                .chatId(id)
                .memberId(memberId)
                .memberDetails(removedMember.getDetails())
                .build());
    }

    @Broadcast
    @Endpoint("/{id}/set_role/{memberId}")
    public Box<ChatRoleMessage> changeMemberRole(@PathVariable String id,
                                                 @PathVariable Long memberId,
                                                 @NonNull UserPrincipal principal,
                                                 @Payload NewChatRole message,
                                                 @NonNull String path) {

        final Long initiatorUserId = principal.token().userId();

        if (initiatorUserId.equals(memberId)) {
            return error(ErrorMessage.create("Invalid user ID", path));
        }

        if (!chatRepository.isMember(id, initiatorUserId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        if (!chatRepository.isMember(id, memberId)) {
            return error(ErrorMessage.create("Member not found", path));
        }

        try {
            chatClient.changeMemberRole(append(principal.rawAccessToken()), id, memberId, message.getRole());
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        return ok(ChatRoleMessage.builder()
                .action(Action.SET_ROLE)
                .chatId(id)
                .senderId(initiatorUserId)
                .memberId(memberId)
                .role(message.getRole())
                .build());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Endpoint("/{id}/remove")
    public Box<Void> removeChat(@PathVariable String id,
                                @NonNull UserPrincipal sender,
                                @NonNull String path) {

        final Long senderId = sender.token().userId();

        if (!chatRepository.isMember(id, senderId)) {
            return error(ErrorMessage.create("You are not member of chat", path));
        }

        // make an api call to delete chat
        try {
            chatClient.removeGroupChat(append(sender.rawAccessToken()), id);
        } catch (ApiErrorException e) {
            return error(ErrorMessage.create(e.getMessage(), path));
        }

        final Set<Long> members = chatRepository.getMembers(id);
        final Set<String> membersSessions = members.stream()
                .map(chatRepository::getUserSessions)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        final String serializedMessage = objectMapper.writeValueAsString(
                ChatMessage.builder()
                        .senderId(senderId)
                        .chatId(id)
                        .action(Action.CHAT_DELETED)
                        .build()
        );

        boolean isSharedCompletely = false;
        try {
            isSharedCompletely = messageBroadcast.share(membersSessions, serializedMessage);
        } catch (IOException e) {
            log.error("removeChat(): {}", e.getMessage());
        }

        if (!isSharedCompletely && isMibEnabled) {

            final var content = objectMapper.readValue(serializedMessage, Map.class);
            content.put("members", members);
            final String newSerializedMessage = objectMapper.writeValueAsString(content);

            for (String instanceId : instances) {
                chatRepository.shareWithConsumer(instanceId, newSerializedMessage);
            }
        }

        chatRepository.deleteChatWithMembers(id, members);

        return Box.ok();
    }

}
