package org.linkwave.ws.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@NoArgsConstructor
@Getter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseMessage {

    private Action action;

    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

}
