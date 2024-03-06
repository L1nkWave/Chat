package org.linkwave.chatservice.chat.group;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.ChatMember;

import java.util.List;

@Getter
@Setter
public class GroupChatDetailsDto {

    private String description;
    private List<ChatMember> members;
    private int membersLimit;
    private boolean isPrivate;

}
