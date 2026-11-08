package org.linkwave.chatservice.invitation;

import lombok.RequiredArgsConstructor;
import org.linkwave.chatservice.api.users.UserDto;
import org.linkwave.chatservice.api.users.UserServiceClient;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.linkwave.chatservice.common.BadRequestDataException;
import org.linkwave.chatservice.common.ChatOptionsViolationException;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.linkwave.chatservice.invitation.InvitationStatus.ACCEPTED;
import static org.linkwave.chatservice.invitation.InvitationStatus.DECLINED;
import static org.linkwave.chatservice.invitation.InvitationStatus.PENDING;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.ws.InvitationNotification;
import org.linkwave.chatservice.api.ws.WSServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final ChatService chatService;
    private final UserServiceClient userServiceClient;
    private final GroupInvitationRepository invitationRepository;
    private final WSServiceClient wsServiceClient;

    /**
     * Any member of a group can invite another user. The invitee must accept before joining.
     */
    public InvitationDto invite(@NonNull RequestInitiator initiator, String chatId, String username) {
        final GroupChat chat = chatService.findGroupChat(chatId);

        if (!chatService.isMember(initiator.userId(), chat)) {
            throw new PrivacyViolationException("Only members can invite others");
        }

        final UserDto invitee = userServiceClient.searchUsers(username, 0, 10, initiator.bearer()).stream()
                .filter(user -> username.equals(user.getUsername()) && !user.isDeleted())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final Long inviteeId = invitee.getId();

        if (inviteeId.equals(initiator.userId())) {
            throw new BadRequestDataException("You cannot invite yourself");
        }
        if (chatService.isMember(inviteeId, chat)) {
            throw new BadRequestDataException("User is already a member");
        }
        if (invitationRepository.existsByChatIdAndInviteeIdAndStatus(chatId, inviteeId, PENDING)) {
            throw new BadRequestDataException("Invitation is already pending");
        }
        if (chat.getMembersCount() >= chat.getMembersLimit()) {
            throw new ChatOptionsViolationException("All members slots are occupied");
        }

        final GroupInvitation saved = invitationRepository.save(GroupInvitation.builder()
                .chatId(chatId)
                .inviterId(initiator.userId())
                .inviteeId(inviteeId)
                .status(PENDING)
                .createdAt(Instant.now())
                .build());

        final InvitationDto dto = toDto(saved, chat, initiator.bearer());
        notifyInvitee(inviteeId, dto, initiator.bearer());
        return dto;
    }

    public List<InvitationDto> listPending(@NonNull RequestInitiator initiator) {
        return invitationRepository.findByInviteeIdAndStatus(initiator.userId(), PENDING).stream()
                .map(invitation -> toDto(
                        invitation,
                        chatService.findGroupChat(invitation.getChatId()),
                        initiator.bearer()))
                .toList();
    }

    public void accept(@NonNull RequestInitiator invitee, String invitationId) {
        final GroupInvitation invitation = findOwnPendingInvitation(invitee.userId(), invitationId);

        // Thêm thành viên trước; nếu lỗi (nhóm đầy, đã là thành viên...) thì lời mời vẫn ở trạng thái PENDING
        chatService.acceptGroupInvitation(invitation.getChatId(), invitation.getInviterId(), invitee);

        try {
            wsServiceClient.loadNewGroupChat(invitee.bearer(), invitation.getChatId());
        } catch (FeignException e) {
            log.warn("cannot register membership in ws-server: {}", e.getMessage());
        }

        try {
            wsServiceClient.notifyMembership(
                    invitee.bearer(),
                    invitation.getChatId(),
                    new WSServiceClient.MembershipNotification("JOIN", invitee.userId(), invitee.userId())
            );
        } catch (FeignException e) {
            log.warn("cannot broadcast join in chat {}: {}", invitation.getChatId(), e.getMessage());
        }

        invitation.setStatus(ACCEPTED);
        invitationRepository.save(invitation);
    }

    public void decline(@NonNull RequestInitiator invitee, String invitationId) {
        final GroupInvitation invitation = findOwnPendingInvitation(invitee.userId(), invitationId);
        invitation.setStatus(DECLINED);
        invitationRepository.save(invitation);
    }

    private void notifyInvitee(Long inviteeId, InvitationDto dto, String bearer) {
        try {
            wsServiceClient.notifyInvitation(bearer, new InvitationNotification(
                    inviteeId, dto.id(), dto.groupId(), dto.groupTitle(), dto.inviter()));
        } catch (FeignException e) {
            log.warn("cannot notify invitee {}: {}", inviteeId, e.getMessage());
        }
    }

    private GroupInvitation findOwnPendingInvitation(Long userId, String invitationId) {
        final GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!userId.equals(invitation.getInviteeId())) {
            throw new PrivacyViolationException("This invitation is not addressed to you");
        }
        if (invitation.getStatus() != PENDING) {
            throw new BadRequestDataException("Invitation is no longer pending");
        }
        return invitation;
    }

    private InvitationDto toDto(GroupInvitation invitation, GroupChat chat, String bearer) {
        final String inviterUsername = userServiceClient
                .getUser(invitation.getInviterId(), bearer)
                .getUsername();

        return new InvitationDto(
                invitation.getId(),
                invitation.getChatId(),
                chat.getName(),
                inviterUsername
        );
    }

}