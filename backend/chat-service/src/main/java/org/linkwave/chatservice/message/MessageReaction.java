package org.linkwave.chatservice.message;

import java.time.Instant;

public record MessageReaction(Long memberId, String reaction, Instant reactedAt) {
}
