package com.chat.wsserver.websocket.repository;

import java.util.Set;

public interface ChatRepository {
    void addMember(long chatId, String userId);
    void removeMember(long chatId, String userId);
    Set<String> getChatMembers(long chatId);
    boolean isMember(long chatId, String userId);
}
