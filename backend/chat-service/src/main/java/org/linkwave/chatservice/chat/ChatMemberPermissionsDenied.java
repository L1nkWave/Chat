package org.linkwave.chatservice.chat;

public class ChatMemberPermissionsDenied extends RuntimeException {
    public ChatMemberPermissionsDenied() {
        super("Permissions denied");
    }
}
