package org.linkwave.chatservice.chat;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.ApiResponseClientErrorException;
import org.linkwave.chatservice.api.ServiceErrorException;
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
import org.linkwave.chatservice.common.*;
import org.linkwave.chatservice.message.Action;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageDto;
import org.linkwave.chatservice.message.MessageService;
import org.linkwave.chatservice.message.member.MemberMessage;
import org.linkwave.chatservice.user.User;
import org.linkwave.chatservice.user.UserService;
import org.linkwave.shared.storage.FileStorageService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static java.lang.String.format;
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
    private final TransactionTemplate txnTemplate;
    private final ModelMapper modelMapper;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private MessageService messageService;

    @Autowired
    public void setMessageService(@Lazy MessageService messageService) {
        this.messageService = messageService;
    }

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

        final Chat newChat;
        try {
            newChat = txnTemplate.execute(txnStatus -> {
                // check if chat already exists
                final var chat = chatRepository.findChatWithPair(initiator.userId(), recipientId);
                if (chat.isPresent()) {
                    throw new BadCredentialsException("Chat already exists");
                }

                // create users if needed
                userService.createUserIfNeed(initiator.userId());
                userService.createUserIfNeed(recipientId);

                // create members
                final var now = Instant.now();
                final List<ChatMember> chatMembers = List.of(
                        new ChatMember(initiator.userId(), ADMIN, now),
                        new ChatMember(recipientId, ADMIN, now)
                );

                // build & save chat
                final Chat _newChat = Chat.builder()
                        .members(chatMembers)
                        .createdAt(now)
                        .build();

                chatRepository.save(_newChat);
                return _newChat;
            });
            Objects.requireNonNull(newChat);
        } catch (TransactionException e) {
            throw new ServiceErrorException(e);
        }

        // load chat to ws server
        try {
            wsServiceClient.loadNewChat(initiator.bearer(), new LoadChatRequest(newChat.getId(), recipientId));
        } catch (FeignException e) {
            log.debug("-> createChat(): chat[{}] not loaded", newChat.getId());
        }
        return modelMapper.map(newChat, ChatDto.class);
    }

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

        try {
            txnTemplate.executeWithoutResult(txnStatus -> {
                userService.createUserIfNeed(initiatorUserId);
                chatRepository.save(newGroupChat);
            });
        } catch (TransactionException e) {
            throw new ServiceErrorException(e);
        }

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
    public Map<String, List<ChatMember>> getChatsMembers(Long userId, List<String> chatId) {
        return chatRepository.findAllById(chatId)
                .stream()
                .filter(chat -> isMember(userId, chat))
                .collect(toMap(Chat::getId, Chat::getMembers));
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
    public boolean isAdmin(Long memberId, @NonNull Chat chat) {
        final Optional<ChatMember> chatMember = findChatMember(memberId, chat);
        return chatMember.isPresent() && chatMember.get().getRole().equals(ADMIN);
    }

    @Override
    public void checkMemberRole(@NonNull Chat chat, Long memberId, ChatRole role) throws ChatMemberPermissionsDenied {
        final Optional<ChatMember> chatMember = findChatMember(memberId, chat);
        if (chatMember.isEmpty() || chatMember.get().getRole() != role) {
            throw new ChatMemberPermissionsDenied();
        }
    }

    @Override
    public Optional<ChatMember> findChatMember(Long userId, @NonNull Chat chat) {
        return chat.getMembers()
                .stream()
                .filter(_chat -> _chat.getId().equals(userId))
                .findAny();
    }

    @Override
    public ChatMemberDto addGroupChatMember(String chatId, @NonNull RequestInitiator initiator) {

        final Long userId = initiator.userId();
        final UserDto user = userServiceClient.getUser(userId, initiator.bearer());

        final GroupChat groupChat = findGroupChat(chatId);
        if (isMember(userId, groupChat)) {
            throw new BadRequestDataException("You are already a member");
        }

        // check chat properties
        if (groupChat.isPrivate()) {
            throw new PrivacyViolationException("Chat is inaccessible at the moment");
        }

        if (groupChat.getMembersCount() == groupChat.getMembersLimit()) {
            throw new ChatOptionsViolationException("All members slots are occupied");
        }

        final ChatMember newMember = groupChat.addMember(userId); // add member

        final var joinMessage = Message.builder()
                .authorId(userId)
                .action(Action.JOIN)
                .chat(groupChat)
                .createdAt(newMember.getJoinedAt())
                .build();

        try {
            txnTemplate.executeWithoutResult(txnStatus -> {
                userService.createUserIfNeed(userId);
                messageService.saveMessage(joinMessage); // save join message
                updateChat(groupChat);
            });
        } catch (TransactionException e) {
            throw new ServiceErrorException(e);
        }

        return ChatMemberDto.builder()
                .id(user.getId())
                .joinedAt(newMember.getJoinedAt())
                .role(newMember.getRole())
                .details(modelMapper.map(user, ChatMemberDetailsDto.class))
                .build();
    }

    @Override
    public ChatMemberDto addGroupChatMember(String chatId, @NonNull RequestInitiator initiator, Long userId) {
        final GroupChat groupChat = findGroupChat(chatId);

        checkMemberRole(groupChat, initiator.userId(), ADMIN);

        if (isMember(userId, groupChat)) {
            throw new BadRequestDataException(format("User[%d] already a member of chat", userId));
        }

        if (groupChat.getMembersCount() == groupChat.getMembersLimit()) {
            throw new ChatOptionsViolationException("All members slots are occupied");
        }

        // check user existence
        final UserDto user = userServiceClient.getUser(userId, initiator.bearer());

        final ChatMember newMember = groupChat.addMember(userId);
        final var message = MemberMessage.builder()
                .authorId(initiator.userId())
                .action(Action.ADD)
                .chat(groupChat)
                .memberId(userId)
                .createdAt(newMember.getJoinedAt())
                .build();

        try {
            txnTemplate.executeWithoutResult(txnStatus -> {
                userService.createUserIfNeed(userId);
                messageService.saveMessage(message);
                updateChat(groupChat);
            });
        } catch (TransactionException e) {
            throw new ServiceErrorException(e);
        }

        return ChatMemberDto.builder()
                .id(user.getId())
                .joinedAt(newMember.getJoinedAt())
                .role(newMember.getRole())
                .details(modelMapper.map(user, ChatMemberDetailsDto.class))
                .build();
    }

    @Transactional
    @Override
    public void removeGroupChatMember(Long userId, String chatId) {
        final User user = userService.getUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final GroupChat groupChat = findGroupChat(chatId);
        if (!isMember(userId, groupChat)) {
            throw new ResourceNotFoundException("Member not found");
        }
        groupChat.removeMember(userId);

        final var leaveMessage = Message.builder()
                .authorId(userId)
                .action(Action.LEAVE)
                .chat(groupChat)
                .build();

        messageService.saveMessage(leaveMessage);
        updateChat(groupChat);

        messageService.removeMessageCursor(user, chatId);
        userService.save(user);
    }

    @Override
    public ChatMemberDto removeGroupChatMember(String chatId, @NonNull RequestInitiator initiator, Long memberId) {

        final User user = userService.getUser(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            txnTemplate.executeWithoutResult(txnStatus -> {
                final GroupChat groupChat = findGroupChat(chatId);

                // check initiator role
                checkMemberRole(groupChat, initiator.userId(), ADMIN);

                // check if given user is chat member
                if (!isMember(memberId, groupChat)) {
                    throw new ResourceNotFoundException("Member not found");
                }

                groupChat.removeMember(memberId);

                final var message = MemberMessage.builder()
                        .action(Action.KICK)
                        .chat(groupChat)
                        .authorId(initiator.userId())
                        .memberId(memberId)
                        .build();

                messageService.saveMessage(message);
                updateChat(groupChat);

                messageService.removeMessageCursor(user, chatId);
                userService.save(user);
            });
        } catch (TransactionException e) {
            throw new ServiceErrorException(e);
        }

        UserDto memberInfo = null;
        try {
            memberInfo = userServiceClient.getUser(memberId, initiator.bearer());
        } catch (ApiResponseClientErrorException e) {
            // user is deleted
        }
        return ChatMemberDto.builder()
                .id(memberId)
                .details(memberInfo == null
                        ? null
                        : modelMapper.map(memberInfo, ChatMemberDetailsDto.class))
                .build();
    }

    @Transactional
    @Override
    public void changeMemberRole(String chatId, Long initiatorId, Long memberId, ChatRole newRole) {
        final GroupChat groupChat = findGroupChat(chatId);
        checkMemberRole(groupChat, initiatorId, ADMIN);

        findChatMember(memberId, groupChat).ifPresentOrElse(
                member -> member.setRole(newRole),
                () -> {
                    throw new ResourceNotFoundException("Member not found");
                }
        );

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
