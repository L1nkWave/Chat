package org.linkwave.ws.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
public class InvitationMessage extends BaseMessage {

    private Long inviteeId;
    private String invitationId;
    private String chatId;
    private String groupTitle;
    private String inviter;

}