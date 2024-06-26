package org.linkwave.ws.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class ChatMessage extends StatusMessage {

    private String chatId;

}
