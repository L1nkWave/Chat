package org.linkwave.chatservice.message;

import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.message.file.CreatedFileMessage;
import org.linkwave.chatservice.message.text.EditTextMessage;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.UpdatedTextMessage;
import org.linkwave.chatservice.user.User;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

public interface MessageService {
    MessageDto saveMessage(Long senderId, String chatId, Action action);

    void saveMessage(Message newMessage);

    MessageDto saveTextMessage(Long senderId, String chatId, @NonNull NewTextMessage messageDto);

    CreatedFileMessage saveFileMessage(RequestInitiator initiator, String chatId, @NonNull MultipartFile attachedFile);

    boolean isOwnFileMessage(Long senderId, String chatId, CreatedFileMessage message);

    byte[] getAttachedFile(Long userId, String messageId);

    UpdatedTextMessage editTextMessage(Long senderId, String messageId, @NonNull EditTextMessage editTextMessage);

    RemovedMessage removeMessage(Long senderId, String messageId);

    void clearMessages(Long senderId, String chatId);

    void updateMessage(@NonNull Message message);

    Message getMessage(String id);

    boolean isMessageSender(Message message, Long memberId);

    Pair<Long, List<MessageDto>> getChatMessages(RequestInitiator initiator, String chatId, int offset, int limit);

    ReadMessages readMessages(Long memberId, String chatId, Instant lastReadMessageTimestamp);

    void addMessageCursor(@NonNull User user, @NonNull ChatMessageCursor cursor);

    void removeMessageCursor(@NonNull User user, String chatId);
}
