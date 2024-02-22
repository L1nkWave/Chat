package org.linkwave.ws.websocket.routing.broadcast.instances;

import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.lang.NonNull;

/**
 * Interface used in {@code MessageListenerAdapter} in order to delegate
 * handling messages received from topic using {@code Redis Pub/Sub}
 *
 * @see MessageListenerAdapter
 */
public interface MessageDelegate {
    void handleMessage(@NonNull String message);
}
