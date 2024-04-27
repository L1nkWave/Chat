package org.linkwave.ws.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
public class OutcomeFileMessage extends IdentifiedMessage {

    @Builder.Default
    private Action action = Action.FILE;
    private String filename;
    private String contentType;
    private long size;

}
