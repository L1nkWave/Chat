package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.DtoConverter;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("messages")
@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"id", "action", "createdAt", "authorId"})
@ToString(exclude = "chat")
@SuperBuilder
public class Message implements DtoConverter<MessageDto> {

    private String id;
    private Action action;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @DBRef
    @JsonIgnore
    private Chat chat;

    private Long authorId;

    @Builder.Default
    private List<MessageReader> readers = new ArrayList<>();

    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, MessageDto.class);
    }

}
