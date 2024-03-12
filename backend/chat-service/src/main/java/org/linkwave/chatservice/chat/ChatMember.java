package org.linkwave.chatservice.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMember {

    private Long id;
    private ChatRole role;
    private Instant joinedAt;

}
