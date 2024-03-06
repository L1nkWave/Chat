package org.linkwave.chatservice.chat.duo;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.ChatMember;
import org.linkwave.chatservice.message.Message;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("chats")
@NoArgsConstructor
@Data
@EqualsAndHashCode(exclude = "messages")
@ToString(exclude = "messages")
@SuperBuilder
public class Chat {

    private String id;

    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();

    @DBRef
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Message lastMessage;

    public enum Type {
        DUO,
        GROUP
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.lastMessage = message;
        message.setChat(this);
    }

}
