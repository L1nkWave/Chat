package com.chat.wsserver.websocket.routing.parser;

import com.chat.wsserver.websocket.routing.RoutingMessage;
import com.chat.wsserver.websocket.routing.exception.InvalidMessageFormatException;

public interface MessageParser {
    RoutingMessage parse(String raw) throws InvalidMessageFormatException;
}
