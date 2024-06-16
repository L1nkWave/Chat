package org.linkwave.chatservice.chat.group;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.ChatDto;
import org.linkwave.chatservice.chat.duo.Chat;

import static org.linkwave.chatservice.chat.duo.Chat.Type.GROUP;
import static org.linkwave.chatservice.common.DtoViews.Detailed;
import static org.linkwave.chatservice.common.DtoViews.New;

@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class GroupChatDto extends ChatDto {

    @JsonView(Detailed.class)
    private Chat.Type type = GROUP;

    @JsonView({New.class, Detailed.class})
    private String name;

}
