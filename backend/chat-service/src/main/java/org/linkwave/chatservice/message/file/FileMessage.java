package org.linkwave.chatservice.message.file;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageDto;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

@Document("messages")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class FileMessage extends Message {

    private String storageFilename;
    private String filename;
    private String contentType;
    private long size;

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, FileMessageDto.class);
    }
}
