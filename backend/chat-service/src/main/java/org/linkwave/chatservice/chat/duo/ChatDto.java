package org.linkwave.chatservice.chat.duo;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

import java.time.Instant;

import static org.linkwave.chatservice.chat.duo.Chat.Type.DUO;
import static org.linkwave.chatservice.common.DtoViews.*;

@Setter
@Getter
public class ChatDto {

    @JsonView(New.class)
    private String id;

    @JsonView(Detailed.class)
    private Chat.Type type = DUO;

    @JsonView({New.class, Detailed.class})
    private Instant createdAt;

    @JsonView(Detailed.class)
    private MessageDto lastMessage;

}
