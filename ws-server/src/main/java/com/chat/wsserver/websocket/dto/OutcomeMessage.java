package com.chat.wsserver.websocket.dto;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record OutcomeMessage(
        Action action,
        Long chatId,
        String sender,
        ZonedDateTime timestamp,
        String text) {
}
