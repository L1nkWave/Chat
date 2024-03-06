package org.linkwave.chatservice.message.poll;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.message.Message;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("messages")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class PollMessage extends Message {

    private String statement;

    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

}
