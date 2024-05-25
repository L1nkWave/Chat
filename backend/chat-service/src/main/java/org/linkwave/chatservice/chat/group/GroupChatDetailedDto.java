package org.linkwave.chatservice.chat.group;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.ChatMemberDto;

import java.util.List;

@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class GroupChatDetailedDto extends GroupChatDto {

    private String description;
    private List<ChatMemberDto> members;
    private int membersLimit;
    private boolean isPrivate;

}
