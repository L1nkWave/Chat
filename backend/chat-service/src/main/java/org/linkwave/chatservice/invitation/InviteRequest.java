package org.linkwave.chatservice.invitation;

import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank String username
) {
}