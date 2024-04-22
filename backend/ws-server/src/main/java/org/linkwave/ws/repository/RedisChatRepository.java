package org.linkwave.ws.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.chat.ChatMember;
import org.modelmapper.internal.Pair;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.linkwave.ws.utils.RedisTemplateUtils.executeInTxn;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository<Long, String> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void loadChats(Map<String, Set<ChatMember>> chatsMembers) {
        executeInTxn(redisTemplate, ops -> {
                    final var setOps = ops.opsForSet();
                    final var hashOps = ops.opsForHash();
                    chatsMembers.forEach((chatId, members) -> {
                        var membersIds = members.stream().map(ChatMember::getId).toList();
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
            executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
                ops.opsForHash().put(userChatsKey(userId), chatId, "0");
                ops.opsForSet().add(chatKey(chatId), valueOf(userId));
            }));
        }
    }

    @Override
    public void removeMember(Long userId, Set<String> chats) {
        executeInTxn(redisTemplate, ops -> chats.forEach(chatId -> {
            ops.opsForHash().delete(userChatsKey(userId), chatId);
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
    public void setUnreadMessages(String chatId, Integer newValue) {
        final Set<Long> members = getMembers(chatId);
        executeInTxn(
                redisTemplate,
                ops -> {
                    final var hashOps = ops.opsForHash();
                    members.forEach(id -> hashOps.put(userChatsKey(id), chatId, String.valueOf(newValue)));
                }
        );
    }

    @Override
    public void changeUnreadMessages(String chatId, Set<Long> membersIds, Integer delta) {
        executeInTxn(
                redisTemplate,
                ops -> {
                    final var hashOps = ops.opsForHash();
                    membersIds.forEach(id -> hashOps.increment(userChatsKey(id), chatId, delta));
                }
        );
    }

    @Override
    public void changeUnreadMessages(String chatId, Long userId, Integer delta) {
        executeInTxn(
                redisTemplate,
                ops -> {
                    final var hashOps = ops.opsForHash();
                    hashOps.increment(userChatsKey(userId), chatId, delta);
                }
        );
    }

    @Override
    public void changeUnreadMessages(String chatId, Long userId, Integer delta, Instant lastReadMessage) {
        executeInTxn(
                redisTemplate,
                ops -> {
                    final String key = userChatsKey(userId);
                    final var hashOps = ops.opsForHash();
                    hashOps.increment(key, chatId, delta);
                    hashOps.put(key, messageCursorKey(chatId), lastReadMessage.toString());
                }
        );
    }

    @Override
    public Map<Long, Instant> getLastReadMessages(String chatId) {
        final Set<Long> members = getMembers(chatId);
        final String cursorKey = messageCursorKey(chatId);
        final var hashOps = redisTemplate.opsForHash();

        return members.stream()
                .map(userId -> Pair.of(userId, hashOps.get(userChatsKey(userId), cursorKey)))
                .filter(pair -> pair.getRight() != null)
                .collect(toMap(Pair::getLeft, pair -> Instant.parse(pair.getRight().toString())));
    }

    @Override
    public Integer getUnreadMessages(String chatId, Long userId) {
        final var hashOps = redisTemplate.opsForHash();
        final var result = hashOps.get(userChatsKey(userId), chatId);
        return result == null ? 0 : Integer.parseInt(result.toString());
    }

    @Override
    public Map<String, Integer> getUnreadMessages(Long userId) {
        final String userChatsKey = userChatsKey(userId);
        final var entries = redisTemplate.opsForHash().entries(userChatsKey);
        return entries.entrySet()
                .stream()
                .filter(e -> !e.getKey().toString().startsWith("c"))
                .map(e -> Map.entry(e.getKey().toString(), Integer.parseInt(e.getValue().toString())))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
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
    public Set<String> getUserSessions(String customKey) {
        final Set<String> members = redisTemplate.opsForSet().members(customKey);
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
    public Set<String> getUserChats(Long userId) {
        final HashOperations<String, String, Integer> hashOps = redisTemplate.opsForHash();
        final String userChatsKey = userChatsKey(userId);
        return TRUE.equals(redisTemplate.hasKey(userChatsKey))
                ? hashOps.keys(userChatsKey)
                : emptySet();
    }

    @Override
    public boolean chatExists(String chatId) {
        return TRUE.equals(redisTemplate.hasKey(chatKey(chatId)));
    }

    @Override
    public void saveSession(Long userId, String sessionId) {
        redisTemplate.opsForSet().add(userKey(userId), sessionId);
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        executeInTxn(redisTemplate, ops -> ops.opsForSet().remove(userKey(userId), sessionId));
    }

    @Override
    public boolean hasSessions(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey(userId)));
    }

    @Override
    public void shareWithConsumer(String consumerId, String jsonMessage) {
        redisTemplate.convertAndSend(consumerId, jsonMessage);
    }

    private String userKey(Long userId) {
        return userKey(String.valueOf(userId));
    }

    private String userKey(String userId) {
        return "user:%s".formatted(userId);
    }

    private String chatKey(String chatId) {
        return "chat:%s".formatted(chatId);
    }

    private String userChatsKey(Long userId) {
        return "user:%d:chats".formatted(userId);
    }

    private String messageCursorKey(String chatId) {
        return "c-%s".formatted(chatId);
    }

    private Set<Long> convertSet(@Nullable Set<String> stringSet) {
        return stringSet == null ? emptySet() :
                stringSet.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
    }

}
