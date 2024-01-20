package org.linkwave.apigateway.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ApiError(
        String path,
        String message,
        int status,
        Instant timestamp) {
}