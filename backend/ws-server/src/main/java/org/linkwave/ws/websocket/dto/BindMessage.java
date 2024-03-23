package org.linkwave.ws.websocket.dto;

public record BindMessage(String chatId, String tmpMessageId, String messageId) {
}
