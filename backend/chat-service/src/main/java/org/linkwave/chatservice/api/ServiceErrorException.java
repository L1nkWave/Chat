package org.linkwave.chatservice.api;

public class ServiceErrorException extends RuntimeException {

    public ServiceErrorException() {
        super();
    }

    public ServiceErrorException(String message) {
        super(message);
    }

    public ServiceErrorException(Throwable cause) {
        super(cause);
    }

}
