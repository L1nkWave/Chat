package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.linkwave.chatservice.chat.MessageAuthorDto;
import static org.linkwave.chatservice.common.DtoViews.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class MessageDto {

    @JsonView({Detailed.class, New.class})
    private String id;

    @JsonView({Detailed.class})
    private Action action;

    @JsonView({Detailed.class, New.class})
    private Instant createdAt;

    @JsonView({Detailed.class})
    private MessageAuthorDto author;

    @JsonProperty("isRead")
    @JsonView({Detailed.class})
    private boolean isRead;

    @JsonView({Detailed.class})
    private List<MessageReaction> reactions;

}
