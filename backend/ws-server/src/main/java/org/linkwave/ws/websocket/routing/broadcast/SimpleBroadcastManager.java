package org.linkwave.ws.websocket.routing.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.linkwave.ws.utils.RouteUtils.getPathVariable;
import static org.linkwave.ws.utils.RouteUtils.isPathVariable;
import static org.springframework.util.ReflectionUtils.*;

@Slf4j
@RequiredArgsConstructor
public class SimpleBroadcastManager implements BroadcastManager {

    @Value("${server.instances.value}")
    protected String instances;

    @Value("${server.instances.separator}")
    protected String separator;

    protected final WebSocketMessageBroadcast messageBroadcast;
    protected final ChatRepository<Long, String> chatRepository;

    @Override
    public void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables,
                        @NonNull Object message, @NonNull String serializedMessage) {

        log.debug("-> process(): routeHandler=[{}.{}]",
                routeHandler.getDeclaringClass().getSimpleName(),
                routeHandler.getName()
        );

        // if it's necessary to broadcast message
        if (!isBroadcast(routeHandler)) {
            return;
        }

        final Broadcast broadcastAnn = routeHandler.getAnnotation(Broadcast.class);
        final String sessionSetKey = resolveKey(broadcastAnn.value(), pathVariables,
                broadcastAnn.analyzeMessage()
                        ? message
                        : null);

        // get all chat members sessions ids
        final Set<String> members = chatRepository.getSessions(sessionSetKey);
        if (members == null || members.isEmpty()) {
            log.warn("-> process(): members not found");
            return;
        }

        broadcast(broadcastAnn, members, serializedMessage);
    }

    protected void broadcast(Broadcast broadcast, Set<String> sessionsIds, String serializedMessage) {
        boolean isSharedCompletely;
        try {
            isSharedCompletely = messageBroadcast.share(sessionsIds, serializedMessage);
        } catch (IOException e) {
            log.error("-> process(): {}", e.getMessage());
            isSharedCompletely = false;
        }

        if (!isSharedCompletely && broadcast.multiInstances()) {
            log.debug("-> process(): multi-instance broadcast is required");

            for (String instanceId : instances.split(separator)) {
                chatRepository.shareWithConsumer(instanceId, serializedMessage);
            }
        }
    }

    @Override
    public boolean isBroadcast(@NonNull Method routeHandler) {
        return routeHandler.isAnnotationPresent(Broadcast.class);
    }

    @NonNull
    protected String resolveKey(@NonNull String keyPattern, @NonNull Map<String, String> pathVariables, Object message) {

        // parse broadcast value pattern
        final String[] components = keyPattern.trim().split(KEY_SEPARATOR);
        final var keyBuilder = new StringBuilder();

        for (String part : components) {
            if (isPathVariable(part)) {
                final String pathVarName = getPathVariable(part);
                String pathVarValue = pathVariables.get(pathVarName);

                if (pathVarValue == null && message != null) {
                    // try to resolve path variable value from message
                    final Field field = findField(message.getClass(), pathVarName);
                    if (field != null) {
                        makeAccessible(field);
                        final Object fieldValue = getField(field, message);
                        pathVarValue = fieldValue == null ? null : fieldValue.toString();
                    }
                }

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
