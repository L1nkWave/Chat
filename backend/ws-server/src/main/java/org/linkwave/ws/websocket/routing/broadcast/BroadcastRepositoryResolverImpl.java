package org.linkwave.ws.websocket.routing.broadcast;

import lombok.RequiredArgsConstructor;
import org.linkwave.ws.repository.ChatRepository;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import static org.linkwave.ws.utils.RouteUtils.isPathVariable;
import static org.linkwave.ws.websocket.routing.broadcast.BroadcastManager.KEY_SEPARATOR;

@RequiredArgsConstructor
public class BroadcastRepositoryResolverImpl implements BroadcastRepositoryResolver {

    private final ChatRepository<Long, String> chatRepository;

    /**
     * If the field type seems a bit complicated, this is an example how this looks like:
     * <pre> {@code
     * return new BroadcastRepositoryResolverImpl(
     *       Map.of(
     *             "user:{}", SessionRepository::getUserSessions,
     *             "chat:{}", ChatRepository::getSessions
     *       )
     * );
     * }</pre>
     */
    private final Map<
            String,
            BiFunction<ChatRepository<Long, String>, String, Set<String>>
            > repositoryResolvers;

    @Override
    public Set<String> resolve(@NonNull String broadcastKeyPattern, @NonNull String resolvedKeyPattern) {
        Objects.requireNonNull(broadcastKeyPattern);
        Objects.requireNonNull(resolvedKeyPattern);

        if (broadcastKeyPattern.equals(resolvedKeyPattern)) {
            throw new IllegalArgumentException("Invalid broadcast keys");
        }

        return repositoryResolvers
                .get(eraseKey(broadcastKeyPattern))
                .apply(chatRepository, resolvedKeyPattern);
    }

    /**
     * Erases key variables names from passed key-pattern.<br/>
     * <b>Example:</b> For key-pattern {@code "chat:{id}"} it returns {@code "chat:{}"}.
     *
     * @param keyPattern non-null string that contains key pattern
     * @return key pattern without its variables names
     */
    @NonNull
    private String eraseKey(@NonNull String keyPattern) {
        final String[] components = keyPattern.trim().split(KEY_SEPARATOR);
        final var sb = new StringBuilder();
        for (String part : components) {
            sb.append(isPathVariable(part) ? "{}" : part).append(KEY_SEPARATOR);
        }
        return sb.substring(0, sb.length() - 1);
    }

}
