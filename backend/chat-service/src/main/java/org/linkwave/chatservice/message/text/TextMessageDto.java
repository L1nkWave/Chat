package org.linkwave.chatservice.message.text;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

@Getter
@Setter
public class TextMessageDto extends MessageDto {

    private String text;
    private boolean isEdited;

}
