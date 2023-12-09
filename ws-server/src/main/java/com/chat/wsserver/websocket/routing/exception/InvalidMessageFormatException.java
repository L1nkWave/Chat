package com.chat.wsserver.websocket.routing.exception;

public class InvalidMessageFormatException extends Exception {

    public InvalidMessageFormatException(String message) {
        super(message);
    }

    public InvalidMessageFormatException(Throwable cause) {
        super(cause);
    }

}
