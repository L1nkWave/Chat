package org.linkwave.chatservice.chat;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChatMemberDto {

    private Long id;
    private ChatRole role;
    private Instant joinedAt;
    private ChatMemberDetailsDto details;

}
