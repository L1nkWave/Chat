package org.linkwave.chatservice.message;

import org.linkwave.chatservice.message.text.EditTextMessage;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.UpdatedTextMessage;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;

public interface MessageService {
    MessageDto saveMessage(Long senderId, String chatId, Action action);

    MessageDto saveTextMessage(Long senderId, String chatId, @NonNull NewTextMessage messageDto);

    UpdatedTextMessage editTextMessage(Long senderId, String messageId, @NonNull EditTextMessage editTextMessage);

    void updateMessage(@NonNull Message message);

    Message getMessage(String id);

    boolean isMessageSender(Message message, Long memberId);

    List<Message> getChatMessages(Long userId, String chatId);

    ReadMessages readMessages(Long memberId, String chatId, Instant lastReadMessageTimestamp);
}
