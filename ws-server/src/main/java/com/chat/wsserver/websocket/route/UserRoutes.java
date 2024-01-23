package com.chat.wsserver.websocket.route;

import com.chat.wsserver.websocket.dto.Action;
import com.chat.wsserver.websocket.dto.StatusMessage;
import com.chat.wsserver.websocket.jwt.UserPrincipal;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import com.chat.wsserver.websocket.routing.broadcast.WebSocketMessageBroadcast;
import com.chat.wsserver.websocket.session.callback.AfterConnectionClosed;
import com.chat.wsserver.websocket.session.callback.AfterConnectionEstablished;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.chat.wsserver.websocket.dto.Action.OFFLINE;
import static com.chat.wsserver.websocket.dto.Action.ONLINE;
import static java.util.stream.Collectors.toSet;

@Slf4j
@WebSocketRoute("/user")
@RequiredArgsConstructor
public class UserRoutes implements AfterConnectionEstablished, AfterConnectionClosed {

    @Value("${server.instances.value}")
    private String instances;

    @Value("${server.instances.separator}")
    private String separator;

    private final WebSocketMessageBroadcast messageBroadcast;
    private final ChatRepository<Long> chatRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnected(@NonNull WebSocketSession session) {
        final String sessionId = session.getId();
        final var principal = (UserPrincipal) session.getPrincipal();
        final Long userId = principal.token().userId();

        log.debug("-> user:[{}] connected, SSID={}", principal.getName(), sessionId);

        final Set<Long> predefinedChats = Set.of(11L, 777L);

        // notify & add connected user to predefined chats
        chatRepository.addMember(userId, predefinedChats);
        notifyChatMembersWith(ONLINE, predefinedChats, userId);
        chatRepository.saveSession(userId, sessionId);
    }

    @Override
    public void afterDisconnected(@NonNull WebSocketSession session) {
        final String sessionId = session.getId();
        final var principal = (UserPrincipal) session.getPrincipal();
        final Long userId = principal.token().userId();

        log.debug("-> user:[{}] disconnected, SSID={}", principal.getName(), sessionId);

        notifyChatMembersWith(OFFLINE, chatRepository.getChats(userId), userId);
        chatRepository.removeSession(userId, sessionId);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void notifyChatMembersWith(Action action, @NonNull Set<Long> chats, Long userId) {

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

            log.debug("-> notifyChatMembersWith(): multi-instance broadcast is required");

            // add field that contains chats ids in order to broadcast message
            // for chat members that are located in other instances
            if (action == OFFLINE) {
                var content = objectMapper.readValue(jsonMessage, Map.class);
                content.put("chats", chats);
                jsonMessage = objectMapper.writeValueAsString(content);
            }

            for (String instanceId : instances.split(separator)) {
                chatRepository.shareWithConsumer(instanceId, jsonMessage);
            }

        }

    }

}
