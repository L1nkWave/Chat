package org.linkwave.chatservice.api;

public class ServiceErrorException extends RuntimeException {

    public ServiceErrorException() {
        super();
    }

    public ServiceErrorException(Throwable cause) {
        super(cause);
    }

}
