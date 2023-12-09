package com.chat.wsserver.unit;

import com.chat.wsserver.websocket.routing.RouteComponent;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import com.chat.wsserver.websocket.routing.WebSocketRouterImpl;
import com.chat.wsserver.websocket.routing.broadcast.BroadcastManager;
import com.chat.wsserver.websocket.routing.broadcast.SimpleBroadcastManager;
import com.chat.wsserver.websocket.routing.broadcast.SimpleMessageBroadcast;
import com.chat.wsserver.websocket.routing.broadcast.WebSocketMessageBroadcast;
import com.chat.wsserver.websocket.routing.parser.TextMessageParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.mockito.Mockito.*;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

public class WebSocketRouterBroadcastTest {

    private static final List<String> SESSIONS = List.of(
            "861a5bbd-7119-d8ba-26e3-65b7a48af37a",
            "111a5bbd-6666-d8ba-26e3-w5c7a55xl34b",
            "111a5bbd-6666-d8ba-26e3-w5c7a559904x",
            "113g5bbd-6666-d8ba-26e3-w5c7a5599ggw",
            "12dfxpsx-6fcc-d8dc-26e3-w5c7a55mxa02"
    );

    private static WebSocketRouter router;
    private static Map<String, WebSocketSession> sessionMap;

    @SneakyThrows
    @BeforeEach
    void init() {

        // prepare sessionMap
        sessionMap = new HashMap<>();

        // mock sessions & put in map
        for (String sessionId : SESSIONS) {
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn(sessionId);
            sessionMap.put(sessionId, session);
        }

        // model chatroom with redis template
        final long chatId = 1;
        final String chatKey = format("chat:%d", chatId);

        var redisTemplate = mock(RedisTemplate.class);
        var setOperations = mock(SetOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForSet().members(chatKey)).thenReturn(
                Set.of(
                        SESSIONS.get(0),
                        SESSIONS.get(1),
                        SESSIONS.get(2)
                )
        );

        // build broadcast components
        WebSocketMessageBroadcast messageBroadcast = new SimpleMessageBroadcast(sessionMap);
        BroadcastManager broadcastManager = new SimpleBroadcastManager(messageBroadcast, redisTemplate);

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        var wsRouter = new WebSocketRouterImpl(new TextMessageParser(), mapper, broadcastManager);

        // build routes
        var chatRoutes = new ChatRoutesBroadcastT();
        Class<?> cls = chatRoutes.getClass();

        // prepare route handlers
        Method sendMessage = cls.getDeclaredMethod("sendMessage", long.class, WebSocketSession.class, String.class);
        makeAccessible(sendMessage);

        Map<String, RouteComponent> routes = Map.of(
                "/group-chat/{id}/send", new RouteComponent(chatRoutes, sendMessage)
        );

        Field routesField = wsRouter.getClass().getDeclaredField("routes");
        makeAccessible(routesField);

        // inject routes
        setField(routesField, wsRouter, routes);

        router = wsRouter;
    }

    @SneakyThrows
    @Test
    void everyoneInChatShouldReceiveTextMessage() {
        final String message = """
                path=/group-chat/1/send
                                
                hello all!!!
                """;

        WebSocketSession session = sessionMap.get(SESSIONS.get(0));
        WebSocketSession session2 = sessionMap.get(SESSIONS.get(1));
        WebSocketSession session3 = sessionMap.get(SESSIONS.get(2));
        WebSocketSession session4 = sessionMap.get(SESSIONS.get(3));

        router.route(message, session);

        verify(session, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));
        verify(session4, never()).sendMessage(any());
    }

}
