package org.linkwave.ws.websocket.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ErrorMessage extends BaseMessage {

    private String error;
    private String path;

    public static ErrorMessage create(String error, String path) {
        return ErrorMessage.builder()
                .action(Action.ERROR)
                .error(error)
                .path(path)
                .build();
    }

}
