package org.linkwave.chatservice.message.file;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

@Getter
@Setter
public class FileMessageDto extends MessageDto {

    private String filename;
    private String contentType;
    private long size;

}
