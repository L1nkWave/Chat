package org.linkwave.chatservice.chat;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.linkwave.chatservice.api.users.UserServiceClient;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.linkwave.chatservice.chat.group.GroupChatDetailsDto;
import org.linkwave.chatservice.chat.group.GroupChatDto;
import org.linkwave.chatservice.chat.group.NewGroupChatRequest;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageService;
import org.linkwave.shared.storage.StorageService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.linkwave.chatservice.chat.ChatRole.ADMIN;
import static org.linkwave.chatservice.message.Action.CREATED;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    public static final Path CHAT_AVATAR_PATH = Path.of("api", "chats");

    private final UserServiceClient userServiceClient;
    private final ChatRepository<Chat> chatRepository;
    private final ModelMapper modelMapper;
    private final StorageService storageService;

    private MessageService messageService;

    @Autowired
    public void setMessageService(@Lazy MessageService messageService) {
        this.messageService = messageService;
    }

    @Transactional
    public ChatDto createChat(@NonNull RequestInitiator initiator,
                              @NonNull NewChatRequest chatRequest) {

        final Long recipientId = chatRequest.getRecipient();

        // forbid creating chat with yourself
        if (initiator.userId().equals(recipientId)) {
            throw new BadCredentialsException("Invalid recipient id");
        }

        // check recipient existence
        userServiceClient.getUser(recipientId, initiator.bearer());

        // check if chat already exists
        final var chat = chatRepository.findChatWithPair(initiator.userId(), recipientId);
        if (chat.isPresent()) {
            throw new BadCredentialsException("Chat already exists");
        }

        final var now = Instant.now();
        final List<ChatMember> chatMembers = List.of(
                new ChatMember(initiator.userId(), ADMIN, now),
                new ChatMember(recipientId, ADMIN, now)
        );

        final Chat newChat = Chat.builder()
                .members(chatMembers)
                .createdAt(now)
                .build();

        // create message that describes chat creation event
        final Message message = messageService.createMessage(initiator.userId(), now, CREATED);
        newChat.getMessages().add(message);
        newChat.setLastMessage(message);

        chatRepository.save(newChat);
        message.setChat(newChat);
        messageService.updateMessage(message);

        return modelMapper.map(newChat, ChatDto.class);
    }

    @Transactional
    public GroupChatDto createGroupChat(@NonNull Long initiatorUserId,
                                        @NonNull NewGroupChatRequest chatRequest) {

        final var now = Instant.now();
        final List<ChatMember> members = List.of(new ChatMember(initiatorUserId, ADMIN, now));

        final GroupChat newGroupChat = GroupChat.builder()
                .name(chatRequest.getName())
                .description(chatRequest.getDescription())
                .isPrivate(chatRequest.getIsPrivate())
                .members(members)
                .membersCount(members.size())
                .createdAt(now)
                .build();

        // create message that describes chat creation event
        final Message message = messageService.createMessage(initiatorUserId, now, CREATED);
        newGroupChat.getMessages().add(message);
        newGroupChat.setLastMessage(message);

        chatRepository.save(newGroupChat);
        message.setChat(newGroupChat);
        messageService.updateMessage(message);

        return modelMapper.map(newGroupChat, GroupChatDto.class);
    }

    public Chat findChat(String id) {
        return chatRepository.findById(id).orElseThrow(ChatNotFoundException::new);
    }

    public GroupChat findGroupChat(String id) {
        return chatRepository.findGroupChatById(id).orElseThrow(ChatNotFoundException::new);
    }

    public Pair<Long, List<ChatDto>> getUserChats(Long userId, int offset, int limit) {

        final List<Chat> userChats = chatRepository.getUserChats(userId, offset, limit);
        final List<ChatDto> selectedChats = userChats
                .stream()
                .map(chat -> {
                    final Class<? extends ChatDto> cls = chat instanceof GroupChat
                            ? GroupChatDto.class
                            : ChatDto.class;
                    final ChatDto chatDto = modelMapper.map(chat, cls);
                    chatDto.setLastMessage(chat.getLastMessage().convert(modelMapper));
                    return chatDto;
                })
                .toList();

        final long chatsTotalCount = chatRepository.getUserChatsTotalCount(userId);

        return Pair.of(chatsTotalCount, selectedChats);
    }

    public void updateChat(@NonNull Chat chat) {
        chatRepository.save(chat);
    }

    public boolean isMember(Long userId, String chatId) {
        return isMember(userId, findChat(chatId));
    }

    public boolean isMember(Long userId, @NonNull Chat chat) {
        return chat.getMembers()
                .stream()
                .map(ChatMember::id)
                .anyMatch(uId -> uId.equals(userId));
    }

    public GroupChatDetailsDto getGroupChatDetails(Long userId, String chatId) {
        final GroupChat chat = findGroupChat(chatId);
        if (!isMember(userId, chat)) {
            throw new PrivacyViolationException();
        }
        return modelMapper.map(chat, GroupChatDetailsDto.class);
    }

    @Transactional
    @SneakyThrows
    @Override
    public void changeGroupChatAvatar(String chatId, @NonNull MultipartFile avatar) {
        final GroupChat chat = findGroupChat(chatId);
        final String filename = storageService.storePicture(CHAT_AVATAR_PATH, chatId, avatar);
        chat.setAvatarPath(filename); // save avatar path
        updateChat(chat);
    }

    @SneakyThrows
    @Override
    public byte[] getGroupChatAvatar(String chatId) {
        final GroupChat groupChat = findGroupChat(chatId);
        if (groupChat.getAvatarPath() == null) {
            throw new ResourceNotFoundException();
        }
        final Path avatarPath = Path.of(CHAT_AVATAR_PATH.toString(), chatId, groupChat.getAvatarPath());
        return storageService.readFileAsBytes(avatarPath);
    }

    @Transactional
    @Override
    public void deleteGroupChatAvatar(String chatId) {
        final GroupChat chat = findGroupChat(chatId);
        chat.setAvatarPath(null);
        updateChat(chat);
    }

}
