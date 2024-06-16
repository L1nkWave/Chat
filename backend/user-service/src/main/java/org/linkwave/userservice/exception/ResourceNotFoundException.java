package org.linkwave.userservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        this("Requested resource not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
