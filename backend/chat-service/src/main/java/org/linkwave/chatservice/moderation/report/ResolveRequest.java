package org.linkwave.chatservice.moderation.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveRequest(
        @NotNull ResolveDecision decision,
        @Size(max = 300) String note
) {
}