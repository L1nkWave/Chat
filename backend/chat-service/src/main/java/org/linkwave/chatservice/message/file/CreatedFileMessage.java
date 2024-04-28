package org.linkwave.chatservice.message.file;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class CreatedFileMessage {
    private String id;
    private Instant createdAt;
    private String filename;
    private String contentType;
    private long size;
}
