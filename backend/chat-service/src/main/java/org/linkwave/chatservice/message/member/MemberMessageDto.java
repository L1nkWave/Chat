package org.linkwave.chatservice.message.member;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

@Getter
@Setter
public class MemberMessageDto extends MessageDto {

    private Long memberId;

}
