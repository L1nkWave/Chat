package org.linkwave.chatservice.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.chat.ChatRole;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.linkwave.chatservice.message.text.EditTextMessage;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.TextMessage;
import org.linkwave.chatservice.message.text.UpdatedTextMessage;
import org.linkwave.chatservice.user.User;
import org.linkwave.chatservice.user.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    private Message createMessage(Long senderId, Instant creationTime, Action action) {
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
    public void saveMessage(@NonNull Message newMessage, @NonNull Chat chat) {
        final Message message = messageRepository.save(newMessage);
        chat.addMessage(message);
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
    public boolean isMessageSender(@NonNull Message message, Long memberId) {
        return message.getAuthorId().equals(memberId);
    }

    @Transactional
    @Override
    public UpdatedTextMessage editTextMessage(Long senderId, String messageId,
                                              @NonNull EditTextMessage editTextMessage) {
        log.debug("-> editTextMessage(): sId={} msgId={}", senderId, messageId);
        final Message message = getMessage(messageId);
        checkPermissions(senderId, message);

        final Chat chat = message.getChat();
        if (message instanceof TextMessage textMessage) {
            textMessage.setText(editTextMessage.getText());
            textMessage.setEditedAt(Instant.now());
            textMessage.setEdited(true);
            updateMessage(textMessage);

            // update last message if needed
            if (message.equals(chat.getLastMessage())) {
                chat.setLastMessage(message);
                chatService.updateChat(chat);
            }

            return UpdatedTextMessage.builder()
                    .messageId(textMessage.getId())
                    .chatId(chat.getId())
                    .text(textMessage.getText())
                    .editedAt(textMessage.getEditedAt())
                    .isEdited(textMessage.isEdited())
                    .build();
        } else {
            log.debug("-> editTextMessage(): not text message sId={} msgId={}", senderId, messageId);
            throw new MessageNotFoundException();
        }
    }

    @Transactional
    @Override
    public RemovedMessage removeMessage(Long senderId, String messageId) {
        final Message message = getMessage(messageId);
        checkPermissions(senderId, message);

        final Chat chat = message.getChat();
        final Message lastMessage = chat.getLastMessage();
        if (message.equals(lastMessage)) {
            chat.setLastMessage(Message.builder()
                    .action(REMOVE)
                    .authorId(lastMessage.getAuthorId())
                    .createdAt(lastMessage.getCreatedAt())
                    .build());
            chatService.updateChat(chat);
        }

        messageRepository.delete(message);

        return RemovedMessage.builder()
                .chatId(chat.getId())
                .messageId(messageId)
                .createdAt(message.getCreatedAt())
                .build();
    }

    @Transactional
    @Override
    public void clearMessages(Long senderId, String chatId) {
        final Chat chat = chatService.findChat(chatId);
        chatService.checkMemberRole(chat, senderId, ChatRole.ADMIN);
        messageRepository.deleteAllChatMessages(chatId);
        chat.setLastMessage(null);
        chatService.updateChat(chat);
    }

    private void checkPermissions(Long userId, @NonNull Message message) {
        final Chat chat = message.getChat();
        if (chatService.isMember(userId, chat) &&
            (isMessageSender(message, userId) || chatService.isAdmin(userId, chat))) {
            return;
        }
        throw new PrivacyViolationException("Do not have permissions to modify the message");
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
    public ReadMessages readMessages(Long memberId, String chatId, Instant lastReadMessageTimestamp) {

        log.debug("-> readMessages(): user[{}] reads chat[{}]", memberId, chatId);

        final User user = userService.getUser(memberId).orElseThrow(ResourceNotFoundException::new);
        final Chat chat = chatService.findChat(chatId);
        if (!chatService.isMember(memberId, chat)) {
            throw new PrivacyViolationException();
        }

        // find chat message cursor
        var optionalCursor = user.getChatMessageCursors()
                .stream()
                .filter(cursor -> cursor.getChatId().equals(chatId))
                .findAny();

        // prepare for querydsl
        final var qMessage = new org.linkwave.chatservice.message.QMessage("message");
        final var isChatMessageAndNotOwn = qMessage.chat.id.eq(chatId).and(qMessage.authorId.ne(memberId));
        final var beforeLastReadMessage = isChatMessageAndNotOwn.andAnyOf(
                qMessage.createdAt.before(lastReadMessageTimestamp),
                qMessage.createdAt.eq(lastReadMessageTimestamp)
        );

        final Iterable<Message> unreadMessages;

        if (optionalCursor.isPresent()) {
            final ChatMessageCursor cursor = optionalCursor.get();
            if (cursor.getTimestamp().equals(lastReadMessageTimestamp)) {
                return ReadMessages.builder().cursor(cursor).build();
            }

            // find unread messages in range (from, to]
            unreadMessages = messageRepository.findAll(
                    beforeLastReadMessage.and(
                            qMessage.createdAt.after(cursor.getTimestamp())
                    )
            );

            // update read message cursor
            cursor.setTimestamp(lastReadMessageTimestamp);
        } else {
            // create read message cursor for chat
            optionalCursor = Optional.of(ChatMessageCursor.builder()
                    .chatId(chatId)
                    .timestamp(lastReadMessageTimestamp)
                    .build());

            addMessageCursor(user, optionalCursor.get());

            // find unread messages starting from the first
            unreadMessages = messageRepository.findAll(beforeLastReadMessage);
        }

        // convert iterable to list
        final List<Message> unreadMessagesList = stream(unreadMessages.spliterator(), false).toList();

        // mark unread messages as read
        final List<Message> readMessages = unreadMessagesList.stream()
                .filter(not(Message::isRead))
                .peek(_message -> _message.setRead(true))
                .toList();

        messageRepository.saveAll(readMessages);
        userService.save(user);

        log.debug("-> readMessages(): user[{}] read {} messages in chat[{}]", memberId, unreadMessagesList.size(), chatId);
        return ReadMessages.builder()
                .cursor(optionalCursor.get())
                .readCount(unreadMessagesList.size())
                .unreadMessages(readMessages.stream().map(Message::getId).toList())
                .build();
    }

    @Override
    public void addMessageCursor(@NonNull User user, @NonNull ChatMessageCursor cursor) {
        user.addChatMessageCursor(cursor);
    }

    @Override
    public void removeMessageCursor(@NonNull User user, String chatId) {
        user.removeChatMessageCursor(chatId);
    }

}
