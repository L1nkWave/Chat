package org.linkwave.ws.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
public class ErrorMessage extends BaseMessage {

    private String error;
    private String path;

    public static ErrorMessage create(String error) {
        return ErrorMessage.builder()
                .action(Action.ERROR)
                .error(error)
                .build();
    }

}
