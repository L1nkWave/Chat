package org.linkwave.ws.unit;

import lombok.experimental.UtilityClass;
import org.linkwave.shared.auth.Token;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Stream.generate;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UtilityClass
public class SessionTestUtils {

    public static Set<String> generateSessionIds(long sessionsCount) {
        return generate(UUID::randomUUID)
                .limit(sessionsCount)
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }

    public static Map<String, WebSocketSession> generateSessionMap(long sessionsCount) {
        return generateSessionIds(sessionsCount)
                .stream()
                .collect(Collectors.toMap(Function.identity(), sessionId -> mock(WebSocketSession.class)));
    }


    @NonNull
    public static Pair<UserPrincipal, WebSocketSession> createSession() {
        return createSession("...", true);
    }

    @NonNull
    public static Pair<UserPrincipal, WebSocketSession> createSession(boolean isBind) {
        return createSession("...", isBind);
    }

    @NonNull
    public static Pair<UserPrincipal, WebSocketSession> createSession(@NonNull String rawToken, boolean isBind) {
        final var token = Token.builder()
                .userId(1L)
                .build();
        final var principal = new UserPrincipal(rawToken, token);

        final var session = mock(WebSocketSession.class);
        if (isBind) {
            when(session.getPrincipal()).thenReturn(principal);
        }

        return Pair.of(principal, session);
    }

}
