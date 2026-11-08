package org.linkwave.ws.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.ws.repository.ChatRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static org.springframework.http.HttpStatus.CREATED;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.InvitationMessage;
import org.linkwave.ws.websocket.dto.MemberMessage;
import org.linkwave.ws.websocket.routing.broadcast.instances.MessageDelegate;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestBody;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Slf4j
@RestController
@RequestMapping("/api/v1/ws-chats")
@RequiredArgsConstructor
public class WSChatsController {

    private final ChatRepository<Long, String> chatRepository;
    private final MessageDelegate messageDelegate;
    private final ObjectMapper objectMapper;

    @PostMapping
    @ResponseStatus(CREATED)
    public void loadNewChat(@RequestBody @Valid LoadChatRequest request) {
        log.debug("-> loadChat()");
        chatRepository.addMember(userDetails().id(), request.getChatId());
        chatRepository.addMember(request.getRecipientId(), request.getChatId());
    }

    @PostMapping("/group/{id}")
    @ResponseStatus(CREATED)
    public void loadNewGroupChat(@PathVariable String id) {
        log.debug("-> loadGroupChat()");
        chatRepository.addMember(userDetails().id(), id);
    }

    @RequestMapping(value = "/{id}/unread_messages", method = {RequestMethod.PATCH, RequestMethod.POST})
    public void addUnreadMessage(@PathVariable String id, @RequestParam Long senderId) {
        final Set<Long> members = chatRepository.getMembers(id);
        members.remove(senderId);
        chatRepository.changeUnreadMessages(id, members, 1);
    }

    @PostMapping("/invitations")
    @ResponseStatus(NO_CONTENT)
    public void notifyInvitation(@RequestBody InvitationNotification notification) {
        final var message = InvitationMessage.builder()
                .action(Action.INVITATION)
                .inviteeId(notification.inviteeId())
                .invitationId(notification.invitationId())
                .chatId(notification.chatId())
                .groupTitle(notification.groupTitle())
                .inviter(notification.inviter())
                .build();
        messageDelegate.handleMessage(objectMapper.writeValueAsString(message));
    }

    // chat-service gọi sau mỗi thay đổi thành viên (REST) để phát realtime tới nhóm
    @PostMapping("/{chatId}/membership")
    @ResponseStatus(NO_CONTENT)
    public void notifyMembership(@PathVariable String chatId,
                                 @RequestBody MembershipNotification notification) {
        final Action action = Action.valueOf(notification.action());
        log.debug("-> notifyMembership(): action={}, chatId={}", action, chatId);

        if (action == Action.JOIN) {
            chatRepository.addMember(notification.memberId(), chatId);
        }

        final var message = MemberMessage.builder()
                .action(action)
                .senderId(notification.initiatorId())
                .chatId(chatId)
                .memberId(notification.memberId())
                .build();

        // phát trước khi gỡ khỏi Redis để người bị kick/rời nhóm cũng nhận được sự kiện
        messageDelegate.handleMessage(objectMapper.writeValueAsString(message));

        if (action == Action.KICK || action == Action.LEAVE) {
            chatRepository.removeMember(notification.memberId(), chatId);
        }
    }

    public record MembershipNotification(String action, Long memberId, Long initiatorId) {}

    private DefaultUserDetails userDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

}