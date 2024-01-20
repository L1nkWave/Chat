package org.linkwave.auth.security.jwt;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record Token(
        UUID id,
        Instant createdAt,
        Instant expireAt,
        Long userId,
        String username,
        List<String> authorities) {
}
