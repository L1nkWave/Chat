package org.linkwave.chatservice.chat;

import java.time.Instant;

public record ChatMember(Long id, ChatRole role, Instant joinedAt) {
}
