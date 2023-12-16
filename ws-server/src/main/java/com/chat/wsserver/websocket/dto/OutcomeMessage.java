package com.chat.wsserver.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
public class OutcomeMessage extends BaseMessage {

    private Long chatId;
    private String sender;
    private String text;

}
