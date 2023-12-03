package com.chat.wsserver.websocket.routing.broadcast;

import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleBroadcastManager implements BroadcastManager {

    private final WebSocketMessageBroadcast messageBroadcast;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables, String jsonMessage) {

        log.debug("-> process(): routeHandler={}", routeHandler.getName());

        Broadcast broadcastAnn = routeHandler.getAnnotation(Broadcast.class);
        String sessionSetKey = resolveKey(broadcastAnn.value(), pathVariables);

        // get all chat members' session ids
        Set<String> members = redisTemplate.opsForSet().members(sessionSetKey);
        if (members == null || members.isEmpty()) {
            log.warn("-> process(): broadcast failed");
            return;
        }

        boolean isSharedCompletely = messageBroadcast.share(members, jsonMessage);
        if (!isSharedCompletely) {
            log.warn("-> process(): message is not shared completely");
        }
    }

    private String resolveKey(String keyPattern, Map<String, String> pathVariables) {

        // parse broadcast value pattern
        String[] components = keyPattern.trim().split(KEY_SEPARATOR);
        var keyBuilder = new StringBuilder();

        for (String part : components) {
            keyBuilder.append(
                    part.startsWith("{") && part.endsWith("}") ?
                            pathVariables.get(part.substring(1, part.length() - 1)) :
                            part
            ).append(":");
        }

        // remove redundant ":" at the end
        keyBuilder.setLength(keyBuilder.length() - 1);
        return keyBuilder.toString();
    }

}
