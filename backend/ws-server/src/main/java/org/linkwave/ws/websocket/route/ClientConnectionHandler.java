package org.linkwave.ws.websocket.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.api.ApiErrorException;
import org.linkwave.ws.api.chat.ChatServiceClient;
import org.linkwave.ws.api.users.UserServiceClient;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.StatusMessage;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import org.linkwave.ws.websocket.session.callback.AfterConnectionClosed;
import org.linkwave.ws.websocket.session.callback.AfterConnectionEstablished;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.linkwave.shared.utils.Bearers.append;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientConnectionHandler implements AfterConnectionEstablished, AfterConnectionClosed {

    @Value("${server.instances.list}")
    private String[] instances;

    @Value("${server.instances.enabled}")
    protected boolean isMultiInstanceBroadcastEnabled;

    private final ChatServiceClient chatClient;
    private final UserServiceClient userClient;
    private final ChatRepository<Long, String> chatRepository;
    private final ObjectMapper objectMapper;
    private WebSocketMessageBroadcast messageBroadcast;

    @Override
    public void afterConnected(@NonNull WebSocketSession session) {
        final String sessionId = session.getId();
        final var principal = (UserPrincipal) session.getPrincipal();
        final Long userId = principal.token().userId();

        log.debug("-> user:[{}] connected, SSID={}", principal.getName(), sessionId);

        if (chatRepository.hasSessions(userId)) {
            chatRepository.saveSession(userId, sessionId);
            return;
        }

        // fetch users chats
        final String bearer = append(principal.rawAccessToken());
        final Set<String> chats;
        try {
            final List<String> chatsIds = chatClient.getUserChats(bearer);
            chats = new HashSet<>(chatsIds);
        } catch (FeignException | ApiErrorException e) {
            log.error("-> afterConnected(): cannot fetch chats, cause=[{}]", e.getMessage());
            throw new IllegalStateException("Chats cannot be resolved", e);
        }

        // find chats that have not loaded yet
        final List<String> nonExistingChats = chats.stream()
                .filter(not(chatRepository::chatExists))
                .toList();

        // load chats with all members
        if (!nonExistingChats.isEmpty()) {
            final var chatsMembers = chatClient.getChatsMembers(bearer, nonExistingChats);
            chatRepository.loadChats(chatsMembers);
        }

        // notify & add connected user to predefined chats
        notifyChatMembersWith(Action.ONLINE, chats, userId);
        chatRepository.saveSession(userId, sessionId);
        userClient.updateUserStatus(userId, Boolean.TRUE);
    }

    @Override
    public void afterDisconnected(@NonNull WebSocketSession session) {
        final String sessionId = session.getId();
        final var principal = (UserPrincipal) session.getPrincipal();
        final Long userId = principal.token().userId();

        log.debug("-> user:[{}] disconnected, SSID={}", principal.getName(), sessionId);

        chatRepository.removeSession(userId, sessionId);

        if (!chatRepository.hasSessions(userId)) {
            notifyChatMembersWith(Action.OFFLINE, chatRepository.getUserChats(userId), userId);
            userClient.updateUserStatus(userId, Boolean.FALSE);
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void notifyChatMembersWith(Action action, @NonNull Set<String> chats, Long userId) {

        String jsonMessage = objectMapper.writeValueAsString(
                StatusMessage.builder()
                        .senderId(userId)
                        .action(action)
                        .build()
        );

        final Set<String> allMembers = chats.stream()
                .map(chatRepository::getChatMembersSessions)
                .map(Set::stream)
                .flatMap(Stream::distinct)
                .collect(toSet());

        /*
            Share user status between chat participants.
            If some sessions are inaccessible from current instance, then
            send message to other instances to continue broadcasting
        */
        if (!messageBroadcast.share(allMembers, jsonMessage)) {

            log.debug("-> notifyChatMembersWith(): {} ID={} mib is required", action, userId);

            // add field that contains chats ids in order to broadcast message
            // for chat members that are located in other instances
            if (action == Action.OFFLINE) {
                var content = objectMapper.readValue(jsonMessage, Map.class);
                content.put("chats", chats);
                jsonMessage = objectMapper.writeValueAsString(content);
            }

            if (isMultiInstanceBroadcastEnabled) {
                for (String instanceId : instances) {
                    chatRepository.shareWithConsumer(instanceId, jsonMessage);
                }
            }
        }

    }

    @Autowired
    public void setMessageBroadcast(@Lazy WebSocketMessageBroadcast messageBroadcast) {
        this.messageBroadcast = messageBroadcast;
    }

}
