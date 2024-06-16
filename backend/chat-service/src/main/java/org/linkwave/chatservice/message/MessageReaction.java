package org.linkwave.chatservice.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MessageReaction {

    private Long memberId;
    private String reaction;
    private Instant reactedAt;

}
