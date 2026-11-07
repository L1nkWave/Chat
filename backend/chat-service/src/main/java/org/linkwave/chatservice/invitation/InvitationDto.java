package org.linkwave.chatservice.invitation;

public record InvitationDto(
        String id,
        String groupId,
        String groupTitle,
        String inviter
) {
}