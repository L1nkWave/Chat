package org.linkwave.chatservice.chat;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.users.UserDto;
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
import org.linkwave.chatservice.message.MessageDto;
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
import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.linkwave.chatservice.chat.ChatRole.ADMIN;
import static org.linkwave.chatservice.common.ListUtils.iterateChunks;
import static org.linkwave.chatservice.message.Action.CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    public static final Path CHAT_AVATAR_PATH = Path.of("api", "chats");
    public static final int DEFAULT_BATCH_SIZE = 50;

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

    @Override
    public Pair<Long, List<ChatDto>> getUserChats(@NonNull RequestInitiator initiator, int offset, int limit) {

        final List<Chat> userChats = chatRepository.getUserChats(initiator.userId(), offset, limit);
        final long chatsTotalCount = chatRepository.getUserChatsTotalCount(initiator.userId());

        final Set<Long> usersIds = new HashSet<>();
        final List<ChatDto> selectedChats = userChats
                .stream()
                .map(chat -> {
                    final Class<? extends ChatDto> cls = chat instanceof GroupChat
                            ? GroupChatDto.class
                            : ChatDto.class;

                    final ChatDto chatDto = modelMapper.map(chat, cls);
                    final Message lastMessage = chat.getLastMessage();
                    final MessageDto messageDto = lastMessage.convert(modelMapper);

                    // save author ID for filling user data in the future
                    messageDto.setAuthor(MessageAuthorDto.builder()
                            .id(lastMessage.getAuthorId())
                            .build());
                    chatDto.setLastMessage(messageDto);

                    usersIds.add(lastMessage.getAuthorId());
                    return chatDto;
                })
                .toList();

        final Map<Long, UserDto> usersMap = new LinkedHashMap<>();

        // pull users
        final int batches = iterateChunks(
                new ArrayList<>(usersIds),
                DEFAULT_BATCH_SIZE,
                ids -> userServiceClient
                        .getUsers(ids, initiator.bearer())
                        .forEach(user -> usersMap.put(user.getId(), user))
        );

        selectedChats.forEach(chat -> {
            final MessageAuthorDto author = chat.getLastMessage().getAuthor();
            final UserDto user = usersMap.get(author.getId());
            if (user != null) { // if user is found, add user details
                author.setUsername(user.getUsername());
                author.setName(user.getName());
            }
        });

        log.debug("-> getUserChats(): performed {} api-requests", batches);

        return Pair.of(chatsTotalCount, selectedChats);
    }

    public void updateChat(@NonNull Chat chat) {
        chatRepository.save(chat);
    }

    public boolean isMember(Long userId, String chatId) {
        return isMember(userId, findChat(chatId));
    }

    public boolean isMember(Long userId, @NonNull Chat chat) {
        return findChatMember(userId, chat).isPresent();
    }

    public Optional<ChatMember> findChatMember(Long userId, @NonNull Chat chat) {
        return chat.getMembers()
                .stream()
                .filter(_chat -> _chat.getId().equals(userId))
                .findAny();
    }

    public GroupChatDetailsDto getGroupChatDetails(@NonNull RequestInitiator initiator, String chatId) {
        final GroupChat chat = findGroupChat(chatId);
        if (!isMember(initiator.userId(), chat)) {
            throw new PrivacyViolationException();
        }

        final List<ChatMemberDto> mappedMembers = new LinkedList<>();

        final int batches = iterateChunks(chat.getMembers(), DEFAULT_BATCH_SIZE, members -> {
            final List<Long> membersIds = members.stream().map(ChatMember::getId).toList();

            // pull users and map it by ID
            final Map<Long, UserDto> usersMap = userServiceClient.getUsers(membersIds, initiator.bearer())
                    .stream()
                    .collect(toMap(UserDto::getId, identity()));

            // map to dto
            for (ChatMember member : members) {
                final UserDto user = usersMap.get(member.getId());
                final ChatMemberDto memberDto = modelMapper.map(member, ChatMemberDto.class);

                // if user was found then set details
                if (user != null) {
                    memberDto.setDetails(modelMapper.map(user, ChatMemberDetailsDto.class));
                }
                mappedMembers.add(memberDto);
            }
        });

        log.debug("-> getGroupChatDetails(): performed {} api-requests", batches);

        final var chatDetails = modelMapper.map(chat, GroupChatDetailsDto.class);
        chatDetails.setMembers(mappedMembers);
        return chatDetails;
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
