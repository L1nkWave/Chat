package org.linkwave.chatservice.chat;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ChatMemberDto {

    private Long id;
    private ChatRole role;
    private Instant joinedAt;
    private ChatMemberDetailsDto details;

}
