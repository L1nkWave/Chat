package org.linkwave.chatservice.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        this("Requested resource not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
