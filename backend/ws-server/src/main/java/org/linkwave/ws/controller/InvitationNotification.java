package org.linkwave.ws.controller;

public record InvitationNotification(
        Long inviteeId,
        String invitationId,
        String chatId,
        String groupTitle,
        String inviter
) {
}