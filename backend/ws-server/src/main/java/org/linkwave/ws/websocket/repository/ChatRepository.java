package org.linkwave.ws.websocket.repository;

import java.util.Set;

public interface ChatRepository<T> extends SessionRepository<T> {
    void addMember(T userId, Set<T> chats);

    default void addMember(T userId, T chatId) {
        addMember(userId, Set.of(chatId));
    }

    void removeMember(T userId, Set<T> chats);

    default void removeMember(T userId, T chatId) {
        removeMember(userId, Set.of(chatId));
    }

    boolean isMember(T chatId, T userId);

    Set<T> getMembers(T chatId);

    Set<T> getChats(T userId);

    void shareWithConsumer(String consumerId, String jsonMessage);
}
