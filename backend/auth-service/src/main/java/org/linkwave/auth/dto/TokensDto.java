package org.linkwave.auth.dto;

import java.time.Instant;

public record TokensDto(Instant refreshExpiration, String accessToken) {
}
