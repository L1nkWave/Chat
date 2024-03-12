package org.linkwave.chatservice.common;

public class UnacceptableRequestDataException extends RuntimeException {

    public UnacceptableRequestDataException() {
        super();
    }

    public UnacceptableRequestDataException(String message) {
        super(message);
    }

}
