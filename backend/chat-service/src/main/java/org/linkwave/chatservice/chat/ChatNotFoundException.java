package org.linkwave.chatservice.chat;

public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException() {
        this("Requested chat not found");
    }

    public ChatNotFoundException(String message) {
        super(message);
    }

}
