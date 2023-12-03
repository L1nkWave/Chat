package com.chat.wsserver.websocket.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;

@Repository
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addMember(long chatId, String userId) {
        String chat = "chat:%d".formatted(chatId);
        String userChats = "user:%s:chats".formatted(userId);

        redisTemplate.opsForSet().add(userChats, valueOf(chatId));
        redisTemplate.opsForSet().add(chat, userId);
    }

    @Override
    public void removeMember(long chatId, String userId) {
        String chat = "chat:%d".formatted(chatId);
        String userChats = "user:%s:chats".formatted(userId);

        redisTemplate.opsForSet().remove(userChats, valueOf(chatId));
        redisTemplate.opsForSet().remove(chat, userId);
    }

    @Override
    public Set<String> getChatMembers(long chatId) {
        return redisTemplate.opsForSet()
                .members("chat:%d".formatted(chatId));
    }

    @Override
    public boolean isMember(long chatId, String userId) {
        String chat = "chat:%d".formatted(chatId);
        return TRUE.equals(redisTemplate.opsForSet().isMember(chat, userId));
    }

}
