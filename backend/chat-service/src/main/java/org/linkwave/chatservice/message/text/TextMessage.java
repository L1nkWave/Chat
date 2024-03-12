package org.linkwave.chatservice.message.text;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageDto;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.time.Instant;

@Document("messages")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class TextMessage extends Message {

    private String text;
    private boolean isEdited;
    private Instant editedAt;

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, TextMessageDto.class);
    }

}
