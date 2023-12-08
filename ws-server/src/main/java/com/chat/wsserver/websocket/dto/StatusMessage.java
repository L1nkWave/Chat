package com.chat.wsserver.websocket.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StatusMessage extends BaseMessage {

    private String user;

}
