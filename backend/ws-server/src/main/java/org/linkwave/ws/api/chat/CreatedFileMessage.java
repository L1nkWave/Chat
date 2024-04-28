package org.linkwave.ws.api.chat;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class CreatedFileMessage {
    private String id;
    private Instant createdAt;
    private String filename;
    private String contentType;
    private long size;
}
