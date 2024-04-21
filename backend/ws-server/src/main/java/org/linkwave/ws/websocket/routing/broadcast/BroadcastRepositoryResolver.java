package org.linkwave.ws.websocket.routing.broadcast;

import org.linkwave.ws.websocket.routing.bpp.Broadcast;

import java.util.Set;

/**
 * Defines which sessions should be retrieved that corresponds to<br/>
 * key-pattern set in {@link Broadcast#value()}.
 */
public interface BroadcastRepositoryResolver {

    /**
     * Retrieves a set of sessions ids based on key-pattern.
     * @param broadcastKeyPattern key-pattern set in {@link Broadcast#value()}
     * @param resolvedKeyPattern key-pattern with resolved key variables
     * @return set of sessions ids that matched the specified criteria
     */
    Set<String> resolve(String broadcastKeyPattern, String resolvedKeyPattern);

}
