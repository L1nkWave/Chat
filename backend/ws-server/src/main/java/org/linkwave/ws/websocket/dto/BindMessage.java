package org.linkwave.ws.websocket.dto;

public record BindMessage(Action action, String chatId, String tmpMessageId, String messageId) {
}
