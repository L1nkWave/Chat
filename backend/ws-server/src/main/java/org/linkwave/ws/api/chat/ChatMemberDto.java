package org.linkwave.ws.api.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Getter
@AllArgsConstructor
public class ChatMemberDto {

    private Long id;
    private String role;
    private Instant joinedAt;

}
