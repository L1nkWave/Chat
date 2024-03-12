package org.linkwave.chatservice.api;

public class ApiResponseClientErrorException extends RuntimeException {
    public ApiResponseClientErrorException(String message) {
        super(message);
    }
}
