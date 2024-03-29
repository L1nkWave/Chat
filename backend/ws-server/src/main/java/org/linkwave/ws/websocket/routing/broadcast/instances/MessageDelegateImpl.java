package org.linkwave.ws.websocket.routing.broadcast.instances;

import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.BaseMessage;
import org.linkwave.ws.websocket.dto.OutcomeMessage;
import org.linkwave.ws.websocket.dto.StatusMessage;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.broadcast.WebSocketMessageBroadcast;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDelegateImpl implements MessageDelegate {

    private final ChatRepository<Long, String> chatRepository;
    private final WebSocketMessageBroadcast messageBroadcast;
    private final ObjectMapper mapper;

    /**
     * @param message must be instanced of {@code BaseMessage}
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public void handleMessage(@NonNull String message) {
        // for now, we have two possible descendants: OutcomeMessage and StatusMessage
        // one of them has chat id, other does not

        // parse message to BaseMessage in order to retrieve action
        BaseMessage baseMessage = mapper.readValue(message, BaseMessage.class);
        Set<String> members;

        Action action = baseMessage.getAction();
        log.debug("-> handleMessage(): action={}", action);

        // then we have to define receivers, in case when we have chat id it is quite obvious
        // but when it is OFFLINE / ONLINE we have to collect all members of user's chats
        switch (action) {
            case JOIN, LEAVE, MESSAGE -> {
                var outcomeMessage = mapper.readValue(message, OutcomeMessage.class);
                members = chatRepository.getChatMembersSessions(outcomeMessage.getChatId());
            }
            case OFFLINE, ONLINE -> {

                var statusMessage = mapper.readValue(message, StatusMessage.class);
                final Set<String> userChats;

                if (action.equals(Action.OFFLINE)) {
                    userChats = new HashSet<>(
                            (List<String>) mapper.readValue(message, Map.class).get("chats")
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
            default -> throw new IllegalStateException("Unpredicted message type");
        }

        messageBroadcast.share(members, message);
    }

}
