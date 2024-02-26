package org.linkwave.ws.websocket.routing.exception;

public class InvalidPathException extends Exception {

    public InvalidPathException(String message) {
        super(message);
    }

    public InvalidPathException(Throwable cause) {
        super(cause);
    }

}
