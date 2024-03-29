package org.linkwave.chatservice.chat;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.users.UserDto;
import org.linkwave.chatservice.api.users.UserServiceClient;
import org.linkwave.chatservice.api.ws.LoadChatRequest;
import org.linkwave.chatservice.api.ws.WSServiceClient;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.linkwave.chatservice.chat.group.GroupChatDetailsDto;
import org.linkwave.chatservice.chat.group.GroupChatDto;
import org.linkwave.chatservice.chat.group.NewGroupChatRequest;
import org.linkwave.chatservice.common.ChatOptionsViolationException;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageDto;
import org.linkwave.shared.storage.FileStorageService;
import org.modelmapper.ModelMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    public static final Path CHAT_AVATAR_PATH = Path.of("api", "chats");
    public static final int DEFAULT_BATCH_SIZE = 50;

    private final UserServiceClient userServiceClient;
    private final WSServiceClient wsServiceClient;
    private final ChatRepository<Chat> chatRepository;
    private final ModelMapper modelMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    @Override
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

        // build & save chat
        final Chat newChat = Chat.builder()
                .members(chatMembers)
                .createdAt(now)
                .build();

        chatRepository.save(newChat);

        // load chat to ws server
        try {
            wsServiceClient.loadNewChat(initiator.bearer(), new LoadChatRequest(newChat.getId(), recipientId));
        } catch (FeignException e) {
            log.debug("-> createChat(): chat[{}] not loaded", newChat.getId());
        }
        return modelMapper.map(newChat, ChatDto.class);
    }

    @Transactional
    @Override
    public GroupChatDto createGroupChat(@NonNull RequestInitiator initiator,
                                        @NonNull NewGroupChatRequest chatRequest) {

        final Long initiatorUserId = initiator.userId();
        final var now = Instant.now();
        final List<ChatMember> members = List.of(new ChatMember(initiatorUserId, ADMIN, now));

        // build & save chat
        final GroupChat newGroupChat = GroupChat.builder()
                .name(chatRequest.getName())
                .description(chatRequest.getDescription())
                .isPrivate(chatRequest.getIsPrivate())
                .members(members)
                .membersCount(members.size())
                .createdAt(now)
                .build();

        chatRepository.save(newGroupChat);

        // load chat to ws server
        try {
            wsServiceClient.loadNewGroupChat(initiator.bearer(), newGroupChat.getId());
        } catch (FeignException e) {
            log.debug("-> createGroupChat(): chat[{}] not loaded", newGroupChat.getId());
        }
        return modelMapper.map(newGroupChat, GroupChatDto.class);
    }

    @Override
    public Chat findChat(String id) {
        return chatRepository.findById(id).orElseThrow(ChatNotFoundException::new);
    }

    @Override
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
                    if (lastMessage == null) {
                        return chatDto;
                    }

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
            final MessageDto lastMessage = chat.getLastMessage();
            if (lastMessage == null) {
                return;
            }
            final MessageAuthorDto author = lastMessage.getAuthor();
            final UserDto user = usersMap.get(author.getId());
            if (user != null) { // if user is found, add user details
                author.setUsername(user.getUsername());
                author.setName(user.getName());
            }
        });

        log.debug("-> getUserChats(): performed {} api-requests", batches);

        return Pair.of(chatsTotalCount, selectedChats);
    }

    @Override
    public List<String> getUserChats(Long userId) {
        return chatRepository.getUserChatsIds(userId)
                .stream()
                .map(Chat::getId)
                .toList();
    }

    @Override
    public void updateChat(@NonNull Chat chat) {
        chatRepository.save(chat);
    }

    @Override
    public boolean isMember(Long userId, String chatId) {
        return isMember(userId, findChat(chatId));
    }

    @Override
    public boolean isMember(Long userId, @NonNull Chat chat) {
        return findChatMember(userId, chat).isPresent();
    }

    @Override
    public Optional<ChatMember> findChatMember(Long userId, @NonNull Chat chat) {
        return chat.getMembers()
                .stream()
                .filter(_chat -> _chat.getId().equals(userId))
                .findAny();
    }

    @Transactional
    @Override
    public ChatMember addGroupChatMember(Long userId, String chatId) {
        final GroupChat groupChat = findGroupChat(chatId);
        if (isMember(userId, groupChat)) {
            throw new IllegalArgumentException("You are already a member");
        }

        // check chat properties
        if (groupChat.isPrivate()) {
            throw new PrivacyViolationException("Chat is inaccessible at the moment");
        }

        if (groupChat.getMembersCount() == groupChat.getMembersLimit()) {
            throw new ChatOptionsViolationException("All members slots are occupied");
        }

        // add member
        final ChatMember newMember = groupChat.addMember(userId);
        updateChat(groupChat);
        return newMember;
    }

    @Transactional
    @Override
    public void removeGroupChatMember(Long userId, String chatId) {
        final GroupChat groupChat = findGroupChat(chatId);
        if (!isMember(userId, groupChat)) {
            throw new ResourceNotFoundException("Member not found");
        }
        groupChat.removeMember(userId);
        updateChat(groupChat);
    }

    @Override
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
        final String filename = fileStorageService.storePicture(CHAT_AVATAR_PATH, chatId, avatar);
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
        return fileStorageService.readFileAsBytes(avatarPath);
    }

    @Transactional
    @Override
    public void deleteGroupChatAvatar(String chatId) {
        final GroupChat chat = findGroupChat(chatId);
        chat.setAvatarPath(null);
        updateChat(chat);
    }

}
