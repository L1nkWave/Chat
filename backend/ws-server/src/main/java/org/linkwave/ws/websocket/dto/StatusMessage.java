package org.linkwave.ws.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class StatusMessage extends BaseMessage {

    private Long senderId;

}
