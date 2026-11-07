package org.linkwave.chatservice.api.ws;

public record InvitationNotification(
        Long inviteeId,
        String invitationId,
        String chatId,
        String groupTitle,
        String inviter
) {
}