package org.linkwave.chatservice.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.DtoViews;
import org.linkwave.chatservice.message.MessageDto;

import java.time.Instant;

import static org.linkwave.chatservice.chat.duo.Chat.Type.DUO;

@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class ChatDto {

    @JsonView(DtoViews.New.class)
    private String id;

    @JsonView(DtoViews.Detailed.class)
    private Chat.Type type = DUO;

    @JsonView({DtoViews.New.class, DtoViews.Detailed.class})
    private Instant createdAt;

    @JsonView(DtoViews.Detailed.class)
    private MessageDto lastMessage;

    @JsonProperty("avatarAvailable")
    @JsonView(DtoViews.Detailed.class)
    private boolean isAvatarAvailable;

}
