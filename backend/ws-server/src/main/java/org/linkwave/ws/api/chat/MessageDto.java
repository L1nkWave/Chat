package org.linkwave.ws.api.chat;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MessageDto {

    private String id;
    private Instant createdAt;

}
