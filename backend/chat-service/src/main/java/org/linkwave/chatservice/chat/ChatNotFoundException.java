package org.linkwave.chatservice.chat;

public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException() {
        this("requested chat not found");
    }

    public ChatNotFoundException(String message) {
        super(message);
    }

}
