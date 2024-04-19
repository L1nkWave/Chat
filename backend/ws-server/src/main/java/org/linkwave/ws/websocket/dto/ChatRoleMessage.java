package org.linkwave.ws.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.linkwave.ws.api.chat.ChatRole;

@NoArgsConstructor
@Getter
@SuperBuilder
public class ChatRoleMessage extends ChatMessage {

    private Long memberId;
    private ChatRole role;

}
