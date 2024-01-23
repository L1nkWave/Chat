package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.jwt.Token;
import com.chat.wsserver.websocket.jwt.UserPrincipal;
import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.DefaultRouteHandlerArgumentResolver;
import com.chat.wsserver.websocket.routing.RouteComponent;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import com.chat.wsserver.websocket.routing.WebSocketRouterImpl;
import com.chat.wsserver.websocket.routing.broadcast.BroadcastManager;
import com.chat.wsserver.websocket.routing.broadcast.SimpleBroadcastManager;
import com.chat.wsserver.websocket.routing.broadcast.SimpleMessageBroadcast;
import com.chat.wsserver.websocket.routing.parser.TextMessageParser;
import com.chat.wsserver.websocket.session.SessionManager;
import com.chat.wsserver.websocket.session.callback.DefaultSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

public class WebSocketRouterBroadcastTest {

    private static final List<String> USERNAMES = of("@user1", "@user2", "@user3", "@user4", "@user5");

    private static final List<Long> USERS_IDS = of(1L, 2L, 3L, 4L, 5L);

    private static final List<String> SESSIONS = of(
            "861a5bbd-7119-d8ba-26e3-65b7a48af37a",
            "111a5bbd-6666-d8ba-26e3-w5c7a55xl34b",
            "111a5bbd-6666-d8ba-26e3-w5c7a559904x",
            "113g5bbd-6666-d8ba-26e3-w5c7a5599ggw",
            "12dfxpsx-6fcc-d8dc-26e3-w5c7a55mxa02"
    );

    private static WebSocketRouter router;
    private static Map<String, WebSocketSession> sessionMap;

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @BeforeEach
    void init() {

        // prepare sessionMap
        sessionMap = new HashMap<>();

        constructPrincipals();

        // model chatroom with chat repository
        final String chatKey = "chat:1";
        final var chatRepository = mock(ChatRepository.class);
        when(chatRepository.getChatMembersSessions(chatKey)).thenReturn(
                Set.of(
                        SESSIONS.get(0),
                        SESSIONS.get(1),
                        SESSIONS.get(2)
                )
        );

        // build broadcast & session manager
        final var messageBroadcast = new SimpleMessageBroadcast();
        messageBroadcast.setSessionManager(buildSessionManager());
        final var broadcastManager = new SimpleBroadcastManager(messageBroadcast, chatRepository);

        router = constructWebSocketRouter(broadcastManager);
    }

    @NonNull
    private SessionManager buildSessionManager() {

        var sessionManager = new DefaultSessionManager(emptyList(), emptyList());

        Class<?> superCls = sessionManager.getClass().getSuperclass();
        List<Field> fields = Arrays.stream(superCls.getDeclaredFields())
                .filter(field -> field.getType().equals(Map.class))
                .filter(field -> {
                    ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                    Type[] types = genericType.getActualTypeArguments();
                    return types[0].equals(String.class) && types[1].equals(WebSocketSession.class);
                }).toList();

        if (fields.size() != 1) {
            throw new RuntimeException(format("[%s] must contain only one session map", superCls.getName()));
        }

        Field sessionsField = fields.get(0);
        makeAccessible(sessionsField);
        setField(sessionsField, sessionManager, sessionMap);

        return sessionManager;
    }

    private void constructPrincipals() {

        // prepare token data
        final var createdAt = Instant.now();
        final var expireAt = createdAt.plus(10L, ChronoUnit.MINUTES);
        final var authorities = of("ROLE_USER");

        // mock sessions & put in map
        for (int i = 0; i < USERNAMES.size(); i++) {

            final var session = mock(WebSocketSession.class);
            final var uuid = UUID.randomUUID();
            final var accessToken = Token.builder()
                    .id(uuid)
                    .username(USERNAMES.get(i))
                    .userId(USERS_IDS.get(i))
                    .authorities(authorities)
                    .createdAt(createdAt)
                    .expireAt(expireAt)
                    .build();

            // mock session's id and principal
            when(session.getId()).thenReturn(SESSIONS.get(i));
            when(session.getPrincipal()).thenReturn(new UserPrincipal(uuid.toString(), accessToken));

            sessionMap.put(SESSIONS.get(i), session);
        }
    }

    @NonNull
    @SneakyThrows
    private WebSocketRouter constructWebSocketRouter(BroadcastManager broadcastManager) {
        // build object mapper
        final var objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // build web socket router
        final var argumentResolver = new DefaultRouteHandlerArgumentResolver(objectMapper);
        final var wsRouter = new WebSocketRouterImpl(
                new TextMessageParser(), argumentResolver,
                objectMapper, broadcastManager
        );

        final var chatRoutes = new ChatRoutesBroadcastT();

        // prepare route handler
        final Method sendMessage = chatRoutes.getClass().getDeclaredMethod(
                "sendMessage",
                long.class,
                WebSocketSession.class,
                String.class
        );
        makeAccessible(sendMessage);

        final Map<String, RouteComponent> routes = Map.of(
                "/group-chat/{id}/send", new RouteComponent(chatRoutes, sendMessage)
        );

        final Field routesField = wsRouter.getClass().getDeclaredField("routes");
        makeAccessible(routesField);

        // inject routes
        setField(routesField, wsRouter, routes);
        return wsRouter;
    }

    @Test
    @DisplayName("Everyone in chat must receive text message")
    @SneakyThrows
    void everyoneInChatMustReceiveTextMessage() {
        final String message = """
                path=/group-chat/1/send
                                
                hello all!!!
                """;

        final WebSocketSession session = sessionMap.get(SESSIONS.get(0));
        final WebSocketSession session2 = sessionMap.get(SESSIONS.get(1));
        final WebSocketSession session3 = sessionMap.get(SESSIONS.get(2));
        final WebSocketSession session4 = sessionMap.get(SESSIONS.get(3));

        router.route(message, session);

        verify(session, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));
        verify(session4, never()).sendMessage(any());
    }

}
