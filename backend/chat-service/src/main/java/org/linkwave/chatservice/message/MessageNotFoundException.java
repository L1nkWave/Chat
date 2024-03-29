package org.linkwave.chatservice.message;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException() {
        this("Requested message not found");
    }

    public MessageNotFoundException(String message) {
        super(message);
    }

}
