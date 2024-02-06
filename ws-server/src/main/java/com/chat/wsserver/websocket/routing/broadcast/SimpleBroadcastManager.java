package com.chat.wsserver.websocket.routing.broadcast;

import com.chat.wsserver.websocket.repository.ChatRepository;
import com.chat.wsserver.websocket.routing.bpp.Broadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleBroadcastManager implements BroadcastManager {

    @Value("${server.instances.value}")
    private String instances;

    @Value("${server.instances.separator}")
    private String separator;

    private final WebSocketMessageBroadcast messageBroadcast;
    private final ChatRepository<Long> chatRepository;

    @Override
    public void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables, String jsonMessage) {

        log.debug("-> process(): routeHandler=[{}.{}]",
                routeHandler.getDeclaringClass().getSimpleName(),
                routeHandler.getName()
        );

        final Broadcast broadcastAnn = routeHandler.getAnnotation(Broadcast.class);

        // broadcast manager determines by itself if it's necessary to broadcast message
        if (broadcastAnn == null) {
            return;
        }

        final String sessionSetKey = resolveKey(broadcastAnn.value(), pathVariables);

        // get all chat members' session ids
        final Set<String> members = chatRepository.getChatMembersSessions(sessionSetKey);
        if (members == null || members.isEmpty()) {
            log.warn("-> process(): members not found");
            return;
        }

        boolean isSharedCompletely;
        try {
            isSharedCompletely = messageBroadcast.share(members, jsonMessage);
        } catch (IOException e) {
            log.error("-> process(): {}", e.getMessage());
            isSharedCompletely = false;
        }

        if (!isSharedCompletely) {
            log.debug("-> process(): multi-instance broadcast is required");

            for (String instanceId : instances.split(separator)) {
                // here we should pass message with session id too
                chatRepository.shareWithConsumer(instanceId, jsonMessage);
            }
        }
    }

    @NonNull
    private String resolveKey(@NonNull String keyPattern, @NonNull Map<String, String> pathVariables) {

        // parse broadcast value pattern
        String[] components = keyPattern.trim().split(KEY_SEPARATOR);
        var keyBuilder = new StringBuilder();

        for (String part : components) {
            if (part.startsWith("{") && part.endsWith("}")) {
                final String pathVarName = part.substring(1, part.length() - 1);
                final String pathVarValue = pathVariables.get(pathVarName);
                if (pathVarValue == null) {
                    throw new IllegalStateException(format("Path variable \"%s\" not found", pathVarName));
                }
                keyBuilder.append(pathVarValue);
            } else {
                keyBuilder.append(part);
            }
            keyBuilder.append(":");
        }

        // remove redundant ":" at the end
        keyBuilder.setLength(keyBuilder.length() - 1);
        return keyBuilder.toString();
    }

}
