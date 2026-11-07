package org.linkwave.ws.websocket.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Short-lived, single-use tickets for the WebSocket handshake.
 * <p>
 * The browser never sends the long-lived access token in the WebSocket URL.
 * Instead it asks for a ticket over HTTPS (Bearer auth), and uses the ticket once
 * within {@link #TTL_SECONDS} seconds. Consumption is atomic (GETDEL), so a ticket
 * can be used at most once even if two handshakes race.
 */
@Service
@RequiredArgsConstructor
public class WsTicketService {

    public static final int TTL_SECONDS = 30;

    private static final String KEY_PREFIX = "ws:ticket:";
    private static final int TICKET_BYTES = 32;
    private static final int MAX_TICKET_LENGTH = 64;

    private static final Duration TTL = Duration.ofSeconds(TTL_SECONDS);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    /**
     * Creates a ticket bound to the given access token.
     *
     * @param rawAccessToken access token already validated by the caller
     * @return opaque ticket string (256 bits of randomness, URL-safe)
     */
    @NonNull
    public String issue(@NonNull String rawAccessToken) {
        final byte[] bytes = new byte[TICKET_BYTES];
        RANDOM.nextBytes(bytes);
        final String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redis.opsForValue().set(KEY_PREFIX + ticket, rawAccessToken, TTL);
        return ticket;
    }

    /**
     * Consumes a ticket. Returns the bound access token only the first time it is called
     * within the TTL; every later call (or an expired/unknown ticket) returns empty.
     */
    @NonNull
    public Optional<String> consume(String ticket) {
        if (ticket == null || ticket.isBlank() || ticket.length() > MAX_TICKET_LENGTH) {
            return Optional.empty();
        }
        return Optional.ofNullable(redis.opsForValue().getAndDelete(KEY_PREFIX + ticket));
    }

}