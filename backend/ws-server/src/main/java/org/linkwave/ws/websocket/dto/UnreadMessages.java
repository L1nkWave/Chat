package org.linkwave.ws.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Getter
@SuperBuilder
public class UnreadMessages extends BaseMessage {

    @Builder.Default
    private Action action = Action.UNREAD_MESSAGES;

    @Builder.Default
    private Map<String, Integer> chats = new HashMap<>();

}
