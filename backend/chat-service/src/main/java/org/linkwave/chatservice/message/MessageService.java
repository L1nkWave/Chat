package org.linkwave.chatservice.message;

import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.message.text.EditTextMessage;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.UpdatedTextMessage;
import org.linkwave.chatservice.user.User;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;

public interface MessageService {
    MessageDto saveMessage(Long senderId, String chatId, Action action);

    void saveMessage(Message newMessage);

    MessageDto saveTextMessage(Long senderId, String chatId, @NonNull NewTextMessage messageDto);

    UpdatedTextMessage editTextMessage(Long senderId, String messageId, @NonNull EditTextMessage editTextMessage);

    RemovedMessage removeMessage(Long senderId, String messageId);

    void clearMessages(Long senderId, String chatId);

    void updateMessage(@NonNull Message message);

    Message getMessage(String id);

    boolean isMessageSender(Message message, Long memberId);

    List<Message> getChatMessages(Long userId, String chatId);

    ReadMessages readMessages(Long memberId, String chatId, Instant lastReadMessageTimestamp);

    void addMessageCursor(@NonNull User user, @NonNull ChatMessageCursor cursor);

    void removeMessageCursor(@NonNull User user, String chatId);
}
