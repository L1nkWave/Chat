package org.linkwave.ws.websocket.repository;

import java.util.Set;

public interface ChatRepository<U, C> extends SessionRepository<U> {
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

    Set<C> getChats(U userId);

    Set<String> getChatMembersSessions(C chatId);

    void shareWithConsumer(String consumerId, String jsonMessage);
}
