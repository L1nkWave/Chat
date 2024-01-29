package com.chat.wsserver.websocket.routing.parser;

import com.chat.wsserver.websocket.routing.RoutingMessage;
import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;

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
