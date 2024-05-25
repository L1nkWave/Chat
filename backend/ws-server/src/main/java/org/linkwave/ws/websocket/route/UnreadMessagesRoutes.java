package org.linkwave.ws.websocket.route;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.dto.UnreadMessages;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.bpp.Endpoint;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@WebSocketRoute("/chat/unread")
@RequiredArgsConstructor
public class UnreadMessagesRoutes {

    private final ChatRepository<Long, String> chatRepository;

    @Endpoint
    public UnreadMessages getUnreadMessages(@NonNull UserPrincipal sender) {
        final Long userId = sender.token().userId();
        return UnreadMessages.builder()
                .chats(chatRepository.getUnreadMessages(userId))
                .build();
    }

    @Endpoint("/{chatId}")
    public UnreadMessages getUnreadMessagesPerChat(@PathVariable String chatId,
                                                   @NonNull UserPrincipal sender) {
        final Long userId = sender.token().userId();
        final Map<String, Integer> readMessagesPerChat = Map.of(
                chatId, chatRepository.getUnreadMessages(chatId, userId)
        );
        return UnreadMessages.builder()
                .chats(readMessagesPerChat)
                .build();
    }

}
