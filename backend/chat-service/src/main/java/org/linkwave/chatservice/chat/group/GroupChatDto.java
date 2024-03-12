package org.linkwave.chatservice.chat.group;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;

import static org.linkwave.chatservice.chat.duo.Chat.Type.GROUP;

@Setter
@Getter
public class GroupChatDto extends ChatDto {

    private Chat.Type type = GROUP;
    private String name;
    private String avatarPath;

}
