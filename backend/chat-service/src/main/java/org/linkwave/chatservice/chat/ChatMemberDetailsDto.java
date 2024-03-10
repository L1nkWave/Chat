package org.linkwave.chatservice.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMemberDetailsDto {

    private String username;
    private String name;
    private String avatarPath;
    private boolean isOnline;

}
