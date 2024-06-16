package org.linkwave.ws.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
public class OutcomeMessage extends IdentifiedMessage {

    private String text;

}
