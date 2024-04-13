package org.linkwave.ws.repository;

import org.linkwave.ws.api.chat.ChatMemberDto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface ChatRepository<U, C> extends SessionRepository<U> {
    void loadChats(Map<C, Set<ChatMemberDto>> chatsMembers);

    void addMember(U userId, Set<C> chats);

    default void addMember(U userId, C chatId) {
        addMember(userId, Set.of(chatId));
    }

    void removeMember(U userId, Set<C> chats);

    default void removeMember(U userId, C chatId) {
        removeMember(userId, Set.of(chatId));
    }

    boolean isMember(C chatId, U userId);

    Set<U> getMembers(C chatId);

    Set<C> getUserChats(U userId);

    boolean chatExists(String chatId);

    Set<String> getChatMembersSessions(C chatId);

    Integer getUnreadMessages(C chatId, U userId);

    Map<String, Integer> getUnreadMessages(U userId);

    void setUnreadMessages(C chatId, Integer newValue);

    void changeUnreadMessages(C chatId, Set<U> membersIds, Integer delta);

    void changeUnreadMessages(C chatId, U userId, Integer delta);

    void changeUnreadMessages(C chatId, U userId, Integer delta, Instant lastReadMessage);

    Map<Long, Instant> getLastReadMessages(C chatId);

    void shareWithConsumer(String consumerId, String jsonMessage);
}
