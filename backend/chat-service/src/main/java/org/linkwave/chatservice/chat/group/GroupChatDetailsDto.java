package org.linkwave.chatservice.chat.group;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.ChatMemberDto;

import java.util.List;

@Getter
@Setter
public class GroupChatDetailsDto {

    private String description;
    private List<ChatMemberDto> members;
    private int membersLimit;
    private boolean isPrivate;

}
