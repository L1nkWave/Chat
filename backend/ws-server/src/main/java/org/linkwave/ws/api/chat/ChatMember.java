package org.linkwave.ws.api.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Getter
public class ChatMember {

    private Long id;
    private String role;
    private Instant joinedAt;

}
