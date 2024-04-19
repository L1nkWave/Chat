package org.linkwave.chatservice.common;

public class BadRequestDataException extends RuntimeException {
    public BadRequestDataException(String message) {
        super(message);
    }
}
