package org.linkwave.auth.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ApiError(
        String path,
        int status,
        String message,
        Instant timestamp) {
}
