package org.linkwave.chatservice.chat;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChatMemberDetailsDto {

    private String username;
    private String name;
    private boolean isAvatarAvailable;
    private boolean isOnline;

}
