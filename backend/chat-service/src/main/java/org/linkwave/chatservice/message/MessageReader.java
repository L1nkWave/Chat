package org.linkwave.chatservice.message;

import java.time.Instant;

public record MessageReader(Long memberId, Instant readAt) {
}
