package org.linkwave.chatservice.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.TextMessage;
import org.linkwave.chatservice.user.User;
import org.linkwave.chatservice.user.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.StreamSupport.stream;
import static org.linkwave.chatservice.message.Action.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatService chatService;
    private final UserService userService;
    private final MessageRepository messageRepository;
    private final ModelMapper modelMapper;

    @Override
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
    @Override
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
    @Override
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

    @Override
    public void updateMessage(@NonNull Message message) {
        messageRepository.save(message);
    }

    @Override
    public List<Message> getChatMessages(Long userId, String chatId) {
        final Chat chat = chatService.findChat(chatId);
        if (!chatService.isMember(userId, chat)) {
            throw new PrivacyViolationException();
        }
        return chat.getMessages().stream()
                .sorted(comparing(Message::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public Message getMessage(String id) {
        return messageRepository.findById(id).orElseThrow(MessageNotFoundException::new);
    }

    @Transactional
    @Override
    public int readMessages(Long memberId, String chatId, String lastReadMessageId) {

        log.debug("-> readMessages(): user[{}] reads chat[{}]", memberId, chatId);

        final User user = userService.getUser(memberId).orElseThrow(ResourceNotFoundException::new);

        final Message lastReadMessage = getMessage(lastReadMessageId);
        final Instant lastReadMessageCreation = lastReadMessage.getCreatedAt();
        if (!lastReadMessage.getChat().getId().equals(chatId)) { // not chat message
            throw new MessageNotFoundException();
        }
        if (!chatService.isMember(memberId, lastReadMessage.getChat())) {
            throw new PrivacyViolationException();
        }

        // find chat message cursor
        final var optionalCursor = user.getChatMessageCursors()
                .stream()
                .filter(cursor -> cursor.getChatId().equals(chatId))
                .findAny();

        // prepare for querydsl
        final var qMessage = new org.linkwave.chatservice.message.QMessage("message");
        final var isChatMessageAndNotOwn = qMessage.chat.id.eq(chatId).and(qMessage.authorId.ne(memberId));

        final Iterable<Message> unreadMessages;

        if (optionalCursor.isPresent()) {
            final ChatMessageCursor cursor = optionalCursor.get();
            if (cursor.getMessageId().equals(lastReadMessageId)) {
                return 0;
            }

            // find unread messages
            unreadMessages = messageRepository.findAll(
                    isChatMessageAndNotOwn
                            .and(qMessage.createdAt.between(
                                    cursor.getMessageCreation(), lastReadMessageCreation
                            ))
            );

            // update read message cursor
            cursor.setMessageId(lastReadMessageId);
            cursor.setMessageCreation(lastReadMessageCreation);
        } else {
            // create read message cursor for chat
            final var newCursor = ChatMessageCursor.builder()
                    .chatId(chatId)
                    .messageId(lastReadMessageId)
                    .messageCreation(lastReadMessageCreation)
                    .build();

            user.addChatMessageCursor(newCursor);

            // find unread messages starting from the first
            unreadMessages = messageRepository.findAll(
                    isChatMessageAndNotOwn
                            .and(qMessage.createdAt.before(lastReadMessageCreation.plusMillis(1L)))
            );
        }

        // mark unread messages as read
        final List<Message> readMessages = stream(unreadMessages.spliterator(), false)
                .filter(not(Message::isRead))
                .peek(_message -> _message.setRead(true))
                .toList();

        final int readMessagesCount = readMessages.size();
        messageRepository.saveAll(readMessages);
        userService.save(user);

        log.debug("-> readMessages(): user[{}] read {} messages in chat[{}]", memberId, readMessagesCount, chatId);
        return readMessagesCount;
    }

}
