package org.linkwave.chatservice.message.poll;

import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.message.MessageDto;

import java.util.Map;

@Getter
@Setter
public class PollMessageDto extends MessageDto {

    private String statement;
    private Map<String, Integer> options;

}
