package org.linkwave.chatservice.moderation.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportCategory category,
        @Size(max = 300) String reason
) {
}