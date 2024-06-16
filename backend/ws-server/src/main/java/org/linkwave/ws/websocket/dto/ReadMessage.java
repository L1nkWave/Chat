package org.linkwave.ws.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@SuperBuilder
public class ReadMessage extends ChatMessage {

    @Builder.Default
    private Action action = Action.READ;

    @Builder.Default
    private List<String> messages = new ArrayList<>();

}
