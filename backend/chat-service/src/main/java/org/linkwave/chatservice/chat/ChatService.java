package org.linkwave.chatservice.chat;

import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.linkwave.chatservice.chat.group.GroupChatDetailsDto;
import org.linkwave.chatservice.chat.group.GroupChatDto;
import org.linkwave.chatservice.chat.group.NewGroupChatRequest;
import org.linkwave.chatservice.common.RequestInitiator;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {
    ChatDto createChat(@NonNull RequestInitiator initiator, @NonNull NewChatRequest chatRequest);

    GroupChatDto createGroupChat(@NonNull Long initiatorUserId, @NonNull NewGroupChatRequest chatRequest);

    Chat findChat(String id);

    GroupChat findGroupChat(String id);

    Pair<Long, List<ChatDto>> getUserChats(Long userId, int offset, int limit);

    void updateChat(@NonNull Chat chat);

    boolean isMember(Long userId, String chatId);

    boolean isMember(Long userId, @NonNull Chat chat);

    GroupChatDetailsDto getGroupChatDetails(Long userId, String chatId);

    void changeGroupChatAvatar(String chatId, @NonNull MultipartFile avatar);

    byte[] getGroupChatAvatar(String chatId);

    void deleteGroupChatAvatar(String chatId);
}
