package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.ChatMemberDetailsDto;
import org.linkwave.chatservice.chat.MessageAuthorDto;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class MessageDto {

    @JsonView({Detailed.class, Created.class})
    private String id;

    @JsonView({Detailed.class})
    private Action action;

    @JsonView({Detailed.class, Created.class})
    private Instant createdAt;

    @JsonView({Detailed.class})
    private MessageAuthorDto author;

    @JsonView({Detailed.class})
    private List<MessageReader> readers;

    @JsonView({Detailed.class})
    private List<MessageReaction> reactions;

    interface Created {
    }

    interface Detailed {
    }

}
