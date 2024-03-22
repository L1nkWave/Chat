package org.linkwave.chatservice.chat.group;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;

import static org.linkwave.chatservice.chat.duo.Chat.Type.GROUP;
import static org.linkwave.chatservice.common.DtoViews.*;

@Setter
@Getter
public class GroupChatDto extends ChatDto {

    @JsonView(Detailed.class)
    private Chat.Type type = GROUP;

    @JsonView({New.class, Detailed.class})
    private String name;

    @JsonView(Detailed.class)
    private String avatarPath;

}
