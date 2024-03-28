package org.linkwave.ws.websocket.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.chat.ChatMemberDto;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static org.linkwave.ws.utils.RedisTemplateUtils.executeInTxn;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository<Long, String> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void loadChats(Map<String, Set<ChatMemberDto>> chatsMembers) {
        executeInTxn(redisTemplate, ops -> {
                    final var setOps = ops.opsForSet();
                    final var hashOps = ops.opsForHash();
                    chatsMembers.forEach((chatId, members) -> {
                        var membersIds = members.stream().map(ChatMemberDto::getId).toList();
                        final String chatKey = chatKey(chatId);

                        // fill chat with members
                        setOps.add(chatKey, membersIds.stream().map(String::valueOf).toArray(String[]::new));

                        // add chat to user chats
                        membersIds.forEach(memberId -> hashOps.put(userChatsKey(memberId), chatId, "0"));
                    });
                }
        );
    }

    @Override
    public void addMember(Long userId, @NonNull Set<String> chats) {
        if (!chats.isEmpty()) {
            RedisTemplateUtils.executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
                ops.opsForSet().add(userChatsKey(userId), valueOf(chatId));
                ops.opsForSet().add(chatKey(chatId), valueOf(userId));
            }));
        }
    }

    @Override
    public void removeMember(Long userId, Set<String> chats) {
        RedisTemplateUtils.executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
            ops.opsForSet().remove(userChatsKey(userId), valueOf(chatId));
            ops.opsForSet().remove(chatKey(chatId), valueOf(userId));
        }));
    }

    @Override
    public Set<Long> getMembers(String chatId) {
        final Set<String> members = redisTemplate.opsForSet().members(chatKey(chatId));
        return convertSet(members);
    }

    @Override
    public Set<String> getChatMembersSessions(String chatId) {
        return getMembers(chatId).stream()
                .map(this::getUserSessions)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSessions(String customKey) {
        Set<Long> members = convertSet(redisTemplate.opsForSet().members(customKey));
        return members.stream()
                .map(this::getUserSessions)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserSessions(Long userId) {
        final Set<String> members = redisTemplate.opsForSet().members(userKey(userId));
        return members == null ? emptySet() : members;
    }

    @Override
    public boolean isMember(String chatId, Long userId) {
        return TRUE.equals(
                redisTemplate.opsForSet().isMember(
                        chatKey(chatId), valueOf(userId)
                )
        );
    }

    @Override
    public Set<String> getChats(Long userId) {
        Set<String> chats = redisTemplate.opsForSet().members(userChatsKey(userId));
        return chats == null ? emptySet() : chats;
    }

    @Override
    public void saveSession(Long userId, String sessionId) {
        redisTemplate.opsForSet().add(userKey(userId), sessionId);
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        final int sessions = getUserSessions(userId).size();
        final Set<String> chats = sessions < 2 ? getChats(userId) : null;

        RedisTemplateUtils.executeInTxn(redisTemplate, ops -> {
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

    private String chatKey(String chatId) {
        return "chat:%s".formatted(chatId);
    }

    private String userChatsKey(Long userId) {
        return "user:%d:chats".formatted(userId);
    }

    private Set<Long> convertSet(@Nullable Set<String> stringSet) {
        return stringSet == null ? emptySet() :
                stringSet.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
    }

}
