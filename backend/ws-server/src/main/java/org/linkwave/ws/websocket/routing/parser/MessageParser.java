package org.linkwave.ws.websocket.routing.parser;

import org.linkwave.ws.websocket.routing.RoutingMessage;
import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;

/**
 * Parses not encoded raw message referring to specified message structure of protocol.
 */
public interface MessageParser {

    /**
     * @param raw not encoded message
     * @return non-null component that contains divided message by path
     * @throws InvalidMessageFormatException if message is invalid and can not be parsed
     * based on specified protocol
     */
    RoutingMessage parse(String raw) throws InvalidMessageFormatException;
}
