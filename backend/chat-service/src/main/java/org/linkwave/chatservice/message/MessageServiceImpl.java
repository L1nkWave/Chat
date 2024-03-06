package org.linkwave.chatservice.message;

import lombok.RequiredArgsConstructor;
import org.linkwave.chatservice.chat.ChatServiceImpl;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.TextMessage;
import org.modelmapper.ModelMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;
import static org.linkwave.chatservice.message.Action.*;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatServiceImpl chatService;
    private final MessageRepository messageRepository;
    private final ModelMapper modelMapper;

    public Message createMessage(Long senderId, Instant creationTime, Action action) {
        final Message message = Message.builder()
                .action(action)
                .authorId(senderId)
                .createdAt(creationTime)
                .build();
        messageRepository.save(message);
        return message;
    }

    @Transactional
    public MessageDto saveMessage(Long senderId, String chatId, Action action) {

        if (action != JOIN && action != LEAVE) {
            throw new UnacceptableRequestDataException("simple message can only contain JOIN or LEAVE as action");
        }

        final Chat chat = chatService.findChat(chatId);
        if (!chatService.isMember(senderId, chat)) {
            throw new PrivacyViolationException();
        }

        final Message message = createMessage(senderId, Instant.now(), action);
        chat.addMessage(message);
        messageRepository.save(message);
        chatService.updateChat(chat);

        return modelMapper.map(message, MessageDto.class);
    }

    @Transactional
    public MessageDto saveTextMessage(Long senderId, String chatId,
                                      @NonNull NewTextMessage messageDto) {

        final Chat chat = chatService.findChat(chatId);
        if (!chatService.isMember(senderId, chat)) {
            throw new PrivacyViolationException();
        }

        final var message = TextMessage.builder()
                .text(messageDto.getText())
                .action(MESSAGE)
                .authorId(senderId)
                .build();

        chat.addMessage(message);
        messageRepository.save(message);
        chatService.updateChat(chat);

        return modelMapper.map(message, MessageDto.class);
    }

    public void updateMessage(@NonNull Message message) {
        messageRepository.save(message);
    }

    public List<Message> getChatMessages(Long userId, String chatId) {
        final Chat chat = chatService.findChat(chatId);
        if (!chatService.isMember(userId, chat)) {
            throw new PrivacyViolationException();
        }
        return chat.getMessages().stream()
                .sorted(comparing(Message::getCreatedAt).reversed())
                .toList();
    }

}
