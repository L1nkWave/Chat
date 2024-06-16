package org.linkwave.ws.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.linkwave.ws.api.chat.ChatMemberDetailsDto;

@NoArgsConstructor
@Getter
@SuperBuilder
public class MemberMessage extends ChatMessage {

    private Long memberId;
    private ChatMemberDetailsDto memberDetails;

}
