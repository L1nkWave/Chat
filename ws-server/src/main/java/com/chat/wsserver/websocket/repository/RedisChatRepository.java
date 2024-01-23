package com.chat.wsserver.websocket.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.chat.wsserver.utils.RedisTemplateUtils.executeInTxn;
import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;

@Repository
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository<Long> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addMember(Long userId, Set<Long> chats) {
        if (getUserSessions(userId).isEmpty()) {
            executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
                ops.opsForSet().add(userChatsKey(userId), valueOf(chatId));
                ops.opsForSet().add(chatKey(chatId), valueOf(userId));
            }));
        }
    }

    @Override
    public void removeMember(Long userId, Set<Long> chats) {
        executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
            ops.opsForSet().remove(userChatsKey(userId), valueOf(chatId));
            ops.opsForSet().remove(chatKey(chatId), valueOf(userId));
        }));
    }

    @Override
    public Set<Long> getMembers(Long chatId) {
        final Set<String> members = redisTemplate.opsForSet().members(chatKey(chatId));
        return convertSet(members);
    }

    @Override
    public Set<String> getChatMembersSessions(Long chatId) {
        return getMembers(chatId).stream()
                .map(this::getUserSessions)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getChatMembersSessions(String customChatKey) {
        Set<Long> members = convertSet(redisTemplate.opsForSet().members(customChatKey));
        return members.stream()
                .map(this::getUserSessions)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserSessions(Long userId) {
        Set<String> members = redisTemplate.opsForSet().members(userKey(userId));
        return members == null ? Collections.emptySet() : members;
    }

    @Override
    public boolean isMember(Long chatId, Long userId) {
        return TRUE.equals(
                redisTemplate.opsForSet().isMember(
                        chatKey(chatId), valueOf(userId)
                )
        );
    }

    @Override
    public Set<Long> getChats(Long userId) {
        Set<String> members = redisTemplate.opsForSet().members(userChatsKey(userId));
        return convertSet(members);
    }

    @Override
    public void saveSession(Long userId, String sessionId) {
        redisTemplate.opsForSet().add(userKey(userId), sessionId);
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        final int sessions = getUserSessions(userId).size();
        final Set<Long> chats = sessions < 2 ? getChats(userId) : null;

        executeInTxn(redisTemplate, ops -> {
            ops.opsForSet().remove(userKey(userId), sessionId);
            if (chats != null) { // need to remove user completely from redis

                // can't use implemented transactional method removeMember(...) here
                chats.forEach(chatId -> {
                    ops.opsForSet().remove(userChatsKey(userId), valueOf(chatId));
                    ops.opsForSet().remove(chatKey(chatId), valueOf(userId));
                });
            }
        });
    }

    @Override
    public void shareWithConsumer(String consumerId, String jsonMessage) {
        redisTemplate.convertAndSend(consumerId, jsonMessage);
    }

    private String userKey(Long userId) {
        return "user:%d".formatted(userId);
    }

    private String chatKey(Long chatId) {
        return "chat:%d".formatted(chatId);
    }

    private String userChatsKey(Long userId) {
        return "user:%d:chats".formatted(userId);
    }

    private Set<Long> convertSet(@Nullable Set<String> stringSet) {
        return stringSet == null ? Collections.emptySet() :
                stringSet.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
    }

}
