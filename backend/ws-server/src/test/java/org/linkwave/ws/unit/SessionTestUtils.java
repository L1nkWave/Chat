package org.linkwave.ws.unit;

import lombok.experimental.UtilityClass;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Stream.generate;
import static org.mockito.Mockito.mock;

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

}
