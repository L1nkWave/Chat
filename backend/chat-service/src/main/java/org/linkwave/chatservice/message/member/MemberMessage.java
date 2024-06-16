package org.linkwave.chatservice.message.member;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.api.users.UserDto;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageDto;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.util.Map;

@Document("messages")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MemberMessage extends Message {

    private Long memberId;

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, MemberMessageDto.class);
    }

    @Override
    public MessageDto mapForFetch(@NonNull ModelMapper modelMapper, Long fetcherUserId,
                                  @NonNull Map<Long, UserDto> users) {

        var message = (MemberMessageDto) super.mapForFetch(modelMapper, fetcherUserId, users);

        final UserDto memberUserDto = users.get(message.getMemberId());
        message.setName(memberUserDto.getName());
        message.setUsername(memberUserDto.getUsername());
        message.setDeleted(memberUserDto.isDeleted());

        return message;
    }
}
