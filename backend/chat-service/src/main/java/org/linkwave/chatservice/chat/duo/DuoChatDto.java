package org.linkwave.chatservice.chat.duo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.ChatDto;

@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class DuoChatDto extends ChatDto {

    private CompanionDto user;

}
