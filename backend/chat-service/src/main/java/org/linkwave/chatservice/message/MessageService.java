package org.linkwave.chatservice.message;

import org.linkwave.chatservice.message.text.NewTextMessage;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;

public interface MessageService {
    Message createMessage(Long senderId, Instant creationTime, Action action);

    MessageDto saveMessage(Long senderId, String chatId, Action action);

    MessageDto saveTextMessage(Long senderId, String chatId, @NonNull NewTextMessage messageDto);

    void updateMessage(@NonNull Message message);

    Message getMessage(String id);

    List<Message> getChatMessages(Long userId, String chatId);

    int readMessages(Long memberId, String chatId, String messageId);
}
