package org.linkwave.ws.websocket.routing;

import org.linkwave.ws.websocket.routing.exception.InvalidMessageFormatException;
import org.linkwave.ws.websocket.routing.exception.InvalidPathException;
import org.springframework.web.socket.WebSocketSession;

public interface WebSocketRouter {

    /**
     *
     * @param message that needs to be delivered to specific route handler
     * @param session message initiator
     * @throws InvalidMessageFormatException if message is unavailable to route
     * @throws InvalidPathException if path is incorrect or does not exist
     */
    void route(String message, WebSocketSession session)
            throws InvalidMessageFormatException, InvalidPathException;

}
