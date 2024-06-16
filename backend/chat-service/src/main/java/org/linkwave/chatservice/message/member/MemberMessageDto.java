package org.linkwave.chatservice.message.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.linkwave.chatservice.message.MessageDto;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MemberMessageDto extends MessageDto {

    private Long memberId;
    private String username;
    private String name;

    @JsonProperty("deleted")
    private boolean isDeleted;

}
