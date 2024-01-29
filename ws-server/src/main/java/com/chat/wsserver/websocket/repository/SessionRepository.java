package com.chat.wsserver.websocket.repository;

import java.util.Set;

public interface SessionRepository<T> {
    Set<String> getUserSessions(T userId);

    void saveSession(T userId, String sessionId);

    void removeSession(T userId, String sessionId);

    Set<String> getChatMembersSessions(T chatId);

    Set<String> getChatMembersSessions(String customChatKey);
}
