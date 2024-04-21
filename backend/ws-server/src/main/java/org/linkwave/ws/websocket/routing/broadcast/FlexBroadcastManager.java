package org.linkwave.ws.websocket.routing.broadcast;

import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.routing.bpp.Broadcast;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
public class FlexBroadcastManager extends SimpleBroadcastManager {

    private final BroadcastRepositoryResolver repositoryResolver;

    public FlexBroadcastManager(WebSocketMessageBroadcast messageBroadcast,
                                ChatRepository<Long, String> chatRepository,
                                BroadcastRepositoryResolver repositoryResolver) {
        super(messageBroadcast, chatRepository);
        this.repositoryResolver = repositoryResolver;
    }

    @Override
    public void process(@NonNull Method routeHandler, @NonNull Map<String, String> pathVariables,
                        @NonNull Object message, @NonNull String serializedMessage) {

        // if it's necessary to broadcast message
        if (!isBroadcast(routeHandler)) {
            return;
        }

        log.debug("-> process(): routeHandler=[{}.{}]",
                routeHandler.getDeclaringClass().getSimpleName(),
                routeHandler.getName()
        );

        Broadcast[] broadcasts = routeHandler.getAnnotationsByType(Broadcast.class);

        if (broadcasts.length > 1) {
            // remove duplicate key-patterns
            final Map<String, Broadcast> broadcastMap = Arrays
                    .stream(broadcasts)
                    .collect(toMap(Broadcast::value, identity(), (b1, b2) -> b1));

            if (broadcastMap.size() != broadcasts.length) {
                log.warn("-> process(): found duplicate key-patterns");
                broadcasts = broadcastMap.values().toArray(new Broadcast[0]);
            }
        }

        for (Broadcast broadcastAnn : broadcasts) {

            final String broadcastKeyPattern = broadcastAnn.value();
            final String resolveBroadcastKey = resolveKey(
                    broadcastKeyPattern,
                    pathVariables,
                    broadcastAnn.analyzeMessage() ? message : null
            );

            // resolve sessions ids
            final Set<String> members = repositoryResolver.resolve(broadcastKeyPattern, resolveBroadcastKey);

            if (members.isEmpty()) {
                log.debug("-> process({}): sessions not found", broadcastKeyPattern);
                continue;
            }

            broadcast(broadcastAnn, members, serializedMessage);
        }
    }

    @Override
    public boolean isBroadcast(@NonNull Method routeHandler) {
        return routeHandler.getAnnotationsByType(Broadcast.class).length != 0;
    }
}
