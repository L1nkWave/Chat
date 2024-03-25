package org.linkwave.ws.websocket.routing.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.linkwave.ws.utils.RouteUtils.getPathVariable;
import static org.linkwave.ws.utils.RouteUtils.isPathVariable;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleBroadcastManager implements BroadcastManager {

    @Value("${server.instances.value}")
    private String instances;

    @Value("${server.instances.separator}")
    private String separator;

    private final WebSocketMessageBroadcast messageBroadcast;
    private final ChatRepository<Long, String> chatRepository;

    @Override
    public void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables, String jsonMessage) {

        log.debug("-> process(): routeHandler=[{}.{}]",
                routeHandler.getDeclaringClass().getSimpleName(),
                routeHandler.getName()
        );

        // if it's necessary to broadcast message
        if (!isBroadcast(routeHandler)) {
            return;
        }

        final Broadcast broadcastAnn = routeHandler.getAnnotation(Broadcast.class);
        final String sessionSetKey = resolveKey(broadcastAnn.value(), pathVariables);

        // get all chat members sessions ids
        final Set<String> members = chatRepository.getSessions(sessionSetKey);
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

    @Override
    public boolean isBroadcast(@NonNull Method routeHandler) {
        return routeHandler.isAnnotationPresent(Broadcast.class);
    }

    @NonNull
    private String resolveKey(@NonNull String keyPattern, @NonNull Map<String, String> pathVariables) {

        // parse broadcast value pattern
        final String[] components = keyPattern.trim().split(KEY_SEPARATOR);
        final var keyBuilder = new StringBuilder();

        for (String part : components) {
            if (isPathVariable(part)) {
                final String pathVarName = getPathVariable(part);
                final String pathVarValue = pathVariables.get(pathVarName);
                if (pathVarValue == null) {
                    throw new IllegalStateException(format("Path variable \"%s\" not found", pathVarName));
                }
                keyBuilder.append(pathVarValue);
            } else {
                keyBuilder.append(part);
            }
            keyBuilder.append(KEY_SEPARATOR);
        }

        // remove redundant ":" at the end
        keyBuilder.setLength(keyBuilder.length() - 1);
        return keyBuilder.toString();
    }

}
