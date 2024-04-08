package org.linkwave.ws.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatMember {

    private Long id;
    private String role;
    private Instant joinedAt;

}
