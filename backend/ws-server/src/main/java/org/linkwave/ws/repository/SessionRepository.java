package org.linkwave.ws.repository;

import java.util.Set;

public interface SessionRepository<T> {
    Set<String> getUserSessions(T userId);

    Set<String> getUserSessions(String customKey);

    void saveSession(T userId, String sessionId);

    void removeSession(T userId, String sessionId);

    boolean hasSessions(T userId);

    Set<String> getSessions(String customKey);
}
