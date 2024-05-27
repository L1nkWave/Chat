package org.linkwave.ws.websocket.routing.broadcast.instances;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.BaseMessage;
import org.linkwave.ws.websocket.dto.ChatMessage;
import org.linkwave.ws.websocket.dto.StatusMessage;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDelegateImpl implements MessageDelegate {

    private static final TypeReference<Map<String, Object>> MESSAGE_CONTENT_TYPE = new TypeReference<>() {};
    private static final String HIDDEN_CHATS_KEY = "chats";
    private static final String HIDDEN_MEMBERS_KEY = "members";

    private final ChatRepository<Long, String> chatRepository;
    private final WebSocketMessageBroadcast messageBroadcast;
    private final ObjectMapper mapper;

    /**
     * @param message serialized message that must be instanced of {@code BaseMessage}.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public void handleMessage(@NonNull String message) {
        // for now, we have two possible descendants: ChatMessage and StatusMessage
        // one of them has chat id, other does not

        // parse message to BaseMessage in order to retrieve action
        final var baseMessage = mapper.readValue(message, BaseMessage.class);
        Set<String> members;

        final Action action = baseMessage.getAction();
        log.debug("-> handleMessage(): action={}", action);

        // then we have to define receivers, in case when we have chat id it is quite obvious
        // but when it is OFFLINE / ONLINE we have to collect all members of user's chats
        switch (action) {
            case OFFLINE, ONLINE -> {

                var statusMessage = mapper.readValue(message, StatusMessage.class);
                final Set<String> userChats;

                if (action.equals(Action.OFFLINE)) {
                    userChats = new HashSet<>(
                            (List<String>) mapper.readValue(message, MESSAGE_CONTENT_TYPE).get(HIDDEN_CHATS_KEY)
                    );
                    message = mapper.writeValueAsString(statusMessage); // remove chats property
                } else {
                    userChats = chatRepository.getUserChats(statusMessage.getSenderId());
                }

                // collect members from user's chats
                members = userChats.stream()
                        .map(chatRepository::getChatMembersSessions)
                        .map(Set::stream)
                        .flatMap(Stream::distinct)
                        .collect(toSet());
            }
            case CHAT_DELETED -> {
                final var content = mapper.readValue(message, MESSAGE_CONTENT_TYPE);

                // get ids from json
                final Set<Long> membersIds = new HashSet<>((
                        ((List<Integer>) content.get(HIDDEN_MEMBERS_KEY)).stream()
                                .map(Integer::longValue)
                                .collect(toSet()))
                );

                // remove members property
                content.remove(HIDDEN_MEMBERS_KEY);
                message = mapper.writeValueAsString(content);

                // collect members' sessions
                members = membersIds.stream()
                        .map(chatRepository::getUserSessions)
                        .flatMap(Set::stream)
                        .collect(toSet());
            }
            case BIND, ERROR -> throw new IllegalStateException("Unsupported message action");
            default -> {
                var chatMessage = mapper.readValue(message, ChatMessage.class);
                members = chatRepository.getChatMembersSessions(chatMessage.getChatId());
            }
        }

        messageBroadcast.share(members, message);
    }

}
