package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.api.users.UserDto;
import org.linkwave.chatservice.chat.MessageAuthorDto;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.DtoConverter;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@QueryEntity
@Document("messages")
@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"id", "action", "createdAt", "authorId"})
@ToString(exclude = "chat")
@SuperBuilder
public class Message implements DtoConverter<MessageDto>, FetchMessageMapping {

    private String id;
    private Action action;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @DBRef
    @JsonIgnore
    private Chat chat;

    private Long authorId;

    private boolean isRead;

    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, MessageDto.class);
    }

    @Override
    public MessageDto mapForFetch(@NonNull ModelMapper modelMapper, Long fetcherUserId,
                                  @NonNull Map<Long, UserDto> users) {
        final MessageDto message = convert(modelMapper);
        final Long authorId = message.getAuthor().getId();
        final UserDto authorUserDto = users.get(authorId);

        if (authorUserDto != null) {
            message.setAuthor(modelMapper.map(authorUserDto, MessageAuthorDto.class));
        }
        if (authorId.equals(fetcherUserId)) {
            message.setIsRead(this.isRead);
        }

        return message;
    }
}
