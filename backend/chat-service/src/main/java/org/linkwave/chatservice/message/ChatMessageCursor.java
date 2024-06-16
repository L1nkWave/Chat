package org.linkwave.chatservice.message;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChatMessageCursor {

    private String chatId;
    private Instant timestamp;

}
