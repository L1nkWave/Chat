package org.linkwave.ws.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class UpdatedTextMessage {

    private String messageId;
    private String chatId;
    private String text;
    private Boolean isEdited;
    private Instant editedAt;

}