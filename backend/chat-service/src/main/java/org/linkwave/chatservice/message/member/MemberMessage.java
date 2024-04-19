package org.linkwave.chatservice.message.member;

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
public class MemberMessage extends Message {

    private Long memberId;

    @Override
    public MessageDto convert(@NonNull ModelMapper modelMapper) {
        return modelMapper.map(this, MemberMessageDto.class);
    }

}
