package com.chat.wsserver.websocket.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.valueOf;

@Repository
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addMember(long chatId, String sessionId) {
        String chat = "chat:%d".formatted(chatId);
        String userChats = "user:%s:chats".formatted(sessionId);

        redisTemplate.opsForSet().add(userChats, valueOf(chatId));
        redisTemplate.opsForSet().add(chat, userId);
    }

    @Override
    public void removeMember(long chatId, String sessionId) {
        String chat = "chat:%d".formatted(chatId);
        String userChats = "user:%s:chats".formatted(sessionId);

        redisTemplate.opsForSet().remove(userChats, valueOf(chatId));
        redisTemplate.opsForSet().remove(chat, userId);
    }

    @Override
    public Set<String> getChatMembers(long chatId) {
        return redisTemplate.opsForSet()
                .members("chat:%d".formatted(chatId));
    }

    @Override
    public boolean isMember(long chatId, String sessionId) {
        String chat = "chat:%d".formatted(chatId);
        return TRUE.equals(redisTemplate.opsForSet().isMember(chat, sessionId));
    }

    @Override
    public Set<Long> getUserChats(String sessionId) {
        String userChats = format("user:%s:chats", sessionId);
        Set<String> members = redisTemplate.opsForSet().members(userChats);

        return members == null ? Collections.emptySet() :
                members.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean clearUserChats(String sessionId) {
        String userChats = format("user:%s:chats", sessionId);
        return redisTemplate.delete(userChats);
    }

}
