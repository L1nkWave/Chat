package org.linkwave.chatservice.user;

import lombok.*;
import org.linkwave.chatservice.message.ChatMessageCursor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"id", "userId"})
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    @Builder.Default
    private List<ChatMessageCursor> chatMessageCursors = new ArrayList<>();

    public void addChatMessageCursor(ChatMessageCursor cursor) {
        chatMessageCursors.add(cursor);
    }

    public boolean removeChatMessageCursor(String chatId) {
        return chatMessageCursors.removeIf(cursor -> cursor.getChatId().equals(chatId));
    }

}
