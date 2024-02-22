package org.linkwave.ws.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.OutcomeMessage;
import org.linkwave.shared.auth.Token;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.*;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.linkwave.ws.websocket.routing.bpp.SubRoute;
import org.linkwave.ws.websocket.routing.bpp.WebSocketRoute;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;
import org.linkwave.ws.websocket.routing.broadcast.SimpleBroadcastManager;
import org.linkwave.ws.websocket.routing.broadcast.SimpleMessageBroadcast;
import org.linkwave.ws.websocket.routing.parser.TextMessageParser;
import org.linkwave.ws.websocket.session.SessionManager;
import org.linkwave.ws.websocket.session.callback.DefaultSessionManager;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
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
import static org.linkwave.ws.unit.SessionTestUtils.generateSessionIds;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

public class WebSocketRouterBroadcastTest {

    private static final List<String> USERNAMES = of("@user1", "@user2", "@user3", "@user4", "@user5");

    private static final List<Long> USERS_IDS = of(1L, 2L, 3L, 4L, 5L);

    private static final List<String> SESSIONS = generateSessionIds(5).stream().toList();

    private static WebSocketRouter router;
    private static Cache<String, WebSocketSession> sessionCache;

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @BeforeEach
    void init() {

        // prepare session cache
        sessionCache = Caffeine.newBuilder().build();

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
        final var messageBroadcast = new SimpleMessageBroadcast(buildSessionManager());
        final var broadcastManager = new SimpleBroadcastManager(messageBroadcast, chatRepository);

        router = constructWebSocketRouter(broadcastManager);
    }

    @NonNull
    private SessionManager buildSessionManager() {

        var sessionManager = new DefaultSessionManager(emptyList(), emptyList());

        Class<?> superCls = sessionManager.getClass().getSuperclass();
        List<Field> fields = Arrays.stream(superCls.getDeclaredFields())
                .filter(field -> field.getType().equals(Cache.class))
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
        setField(sessionsField, sessionManager, sessionCache);

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

            sessionCache.put(SESSIONS.get(i), session);
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

        final WebSocketSession session = sessionCache.getIfPresent(SESSIONS.get(0));
        final WebSocketSession session2 = sessionCache.getIfPresent(SESSIONS.get(1));
        final WebSocketSession session3 = sessionCache.getIfPresent(SESSIONS.get(2));
        final WebSocketSession session4 = sessionCache.getIfPresent(SESSIONS.get(3));

        router.route(message, session);

        verify(session, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));
        verify(session4, never()).sendMessage(any());
    }

    @WebSocketRoute("/group-chat")
    private static class ChatRoutesBroadcastT {

        @SubRoute("/{id}/send")
        @Broadcast("chat:{id}")
        Box<OutcomeMessage> sendMessage(@PathVariable("id") long id,
                                        @NonNull WebSocketSession session,
                                        @Payload String message) {
            final var principal = (UserPrincipal) session.getPrincipal();
            assert principal != null;
            return Box.ok(OutcomeMessage.builder()
                    .action(Action.MESSAGE)
                    .chatId(id)
                    .senderId(principal.token().userId())
                    .text(message)
                    .build());
        }

    }

}
