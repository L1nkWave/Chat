package com.chat.wsserver.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Getter
@SuperBuilder
public class BaseMessage {

    private Action action;

    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

}
