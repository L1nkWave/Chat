package org.linkwave.ws.api.chat;

import lombok.*;

import java.time.Instant;

@Getter
public class ChatMessageCursor {

    private String chatId;
    private Instant timestamp;

}
