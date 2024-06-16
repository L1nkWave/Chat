package org.linkwave.chatservice.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ReadMessages {

    private ChatMessageCursor cursor;

    /**
     * General amount of messages that user has not read yet.
     */
    private int readCount;

    /**
     * Messages that no one has read yet.
     */
    @Builder.Default
    private List<String> unreadMessages = new ArrayList<>();

}
