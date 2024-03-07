package org.linkwave.chatservice.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Requested resource not found");
    }
}
