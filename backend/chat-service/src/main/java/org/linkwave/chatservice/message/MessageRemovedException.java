package org.linkwave.chatservice.message;

public class MessageRemovedException extends RuntimeException {

    public MessageRemovedException() {
        super("Tin nhắn đã bị xóa");
    }

}
