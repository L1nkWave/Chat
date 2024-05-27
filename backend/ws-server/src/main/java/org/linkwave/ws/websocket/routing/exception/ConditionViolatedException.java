package org.linkwave.ws.websocket.routing.exception;

public class ConditionViolatedException extends RuntimeException {
    public ConditionViolatedException(String message) {
        super(message);
    }
}
