package com.chat.wsserver.websocket.route;

import com.chat.wsserver.websocket.dto.Action;
import com.chat.wsserver.websocket.dto.StatusMessage;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.bpp.SubRoute;
import com.chat.wsserver.websocket.routing.bpp.WebSocketRoute;
import com.chat.wsserver.websocket.routing.broadcast.WebSocketMessageBroadcast;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.chat.wsserver.websocket.dto.Action.OFFLINE;
import static com.chat.wsserver.websocket.dto.Action.ONLINE;
import static java.util.stream.Collectors.toSet;

@Slf4j
@WebSocketRoute("/user")
@RequiredArgsConstructor
public class UserRoutes {

    private final WebSocketMessageBroadcast messageBroadcast;
    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;

    @SubRoute(value = "/connected", disabled = true)
    void handleUserConnected(@NonNull WebSocketSession session) {
        String sessionId = session.getId();
        log.debug("-> handleUserConnected(): ss={}", sessionId);

        final List<Long> predefinedChats = List.of(11L, 777L);

        // notify & add connected user to predefined chats
        notifyChatMembersWith(
                predefinedChats,
                ONLINE,
                sessionId,
                chatRepository::addMember
        );

    }

    @SubRoute(value = "/disconnected", disabled = true)
    public void handleUserDisconnected(@NonNull WebSocketSession session) {
        String sessionId = session.getId();
        log.debug("-> handleUserDisconnected(): ss={}", sessionId);

        // notify & remove disconnected user from his chats
        notifyChatMembersWith(
                chatRepository.getUserChats(sessionId),
                OFFLINE,
                sessionId,
                chatRepository::removeMember
        );

        // remove session from redis
        chatRepository.clearUserChats(sessionId);
    }

    @SneakyThrows
    private void notifyChatMembersWith(@NonNull Collection<Long> chats,
                                       Action action,
                                       String sessionId,
                                       BiConsumer<Long, String> biConsumer) {

        String jsonMessage = objectMapper.writeValueAsString(
                StatusMessage.builder()
                        .user(sessionId)
                        .action(action)
                        .build()
        );

        Set<String> allMembers = chats.stream()
                .map(chatRepository::getChatMembers)
                .map(Set::stream)
                .flatMap(Stream::distinct)
                .collect(toSet());

        // share user status between chat participants
        messageBroadcast.share(allMembers, jsonMessage);

        // iterate through user's chats
        for (Long chatId : chats) {

            // perform operation between user and his chats
            biConsumer.accept(chatId, sessionId);

        }

    }

}
