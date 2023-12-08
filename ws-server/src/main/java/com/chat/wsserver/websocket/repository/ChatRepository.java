package com.chat.wsserver.websocket.repository;

import java.util.Set;

public interface ChatRepository {
    void addMember(long chatId, String sessionId);
    void removeMember(long chatId, String sessionId);
    Set<String> getChatMembers(long chatId);
    boolean isMember(long chatId, String sessionId);
    Set<Long> getUserChats(String sessionId);
    boolean clearUserChats(String sessionId);
}
