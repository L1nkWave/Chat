package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

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
    private Long authorId;

    @JsonView({Detailed.class})
    private List<MessageReader> readers;

    @JsonView({Detailed.class})
    private List<MessageReaction> reactions;

    interface Created {
    }

    interface Detailed {
    }

}
