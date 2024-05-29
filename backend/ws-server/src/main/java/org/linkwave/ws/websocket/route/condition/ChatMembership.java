package org.linkwave.ws.websocket.route.condition;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.EndpointCondition;
import org.linkwave.ws.websocket.routing.MessageContext;
import org.linkwave.ws.websocket.routing.exception.ConditionViolatedException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMembership implements EndpointCondition {

    public static final String[] CHAT_ID_PROPERTIES = {"id", "chatId"};

    private final ChatRepository<Long, String> chatRepository;

    @Override
    public void check(@NonNull MessageContext context) throws ConditionViolatedException {
        final var user = (UserPrincipal) context.session().getPrincipal();
        final Long userId = user.token().userId();

        String chatId = null;

        // find chat id
        for (String property : CHAT_ID_PROPERTIES) {
            chatId = context.pathVariables().get(property);
            if (chatId != null) {
                break;
            }
        }

        if (chatId == null || !chatRepository.isMember(chatId, userId)) {
            throw new ConditionViolatedException("You are not member of chat");
        }
    }

}
