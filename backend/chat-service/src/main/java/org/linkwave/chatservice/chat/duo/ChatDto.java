package org.linkwave.chatservice.chat.duo;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

import java.time.Instant;

import static org.linkwave.chatservice.chat.duo.Chat.Type.DUO;

@Setter
@Getter
public class ChatDto {

    private String id;
    private Chat.Type type = DUO;
    private Instant createdAt;
    private MessageDto lastMessage;

}
