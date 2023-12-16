package com.chat.wsserver.websocket.routing.broadcast.instances;

import com.chat.wsserver.websocket.dto.Action;
import com.chat.wsserver.websocket.dto.BaseMessage;
import com.chat.wsserver.websocket.dto.OutcomeMessage;
import com.chat.wsserver.websocket.dto.StatusMessage;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.broadcast.WebSocketMessageBroadcast;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static com.chat.wsserver.websocket.dto.Action.OFFLINE;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDelegateImpl implements MessageDelegate {

    private final ChatRepository chatRepository;
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
                members = chatRepository.getChatMembers(outcomeMessage.getChatId());
            }
            case OFFLINE, ONLINE -> {

                var statusMessage = mapper.readValue(message, StatusMessage.class);
                Set<Long> userChats;

                if (action.equals(OFFLINE)) {
                    Set<Number> chats = new HashSet<>(
                            (List<Number>) mapper.readValue(message, Map.class).get("chats")
                    );

                    // recover parametrized type of set after deserialization
                    userChats = chats.stream()
                            .map(Number::longValue)
                            .collect(toSet());

                    message = mapper.writeValueAsString(statusMessage); // remove chats property
                    System.out.println();
                } else {
                    userChats = chatRepository.getUserChats(statusMessage.getUser());
                }

                // collect members from user's chats
                members = userChats.stream()
                        .map(chatRepository::getChatMembers)
                        .map(Set::stream)
                        .flatMap(Stream::distinct)
                        .collect(toSet());
            }
            default -> throw new IllegalStateException("Unpredicted message type");
        }

        messageBroadcast.share(members, message);
    }

}
