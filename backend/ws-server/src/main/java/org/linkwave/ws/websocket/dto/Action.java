package org.linkwave.ws.websocket.dto;

public enum Action {
    JOIN, LEAVE,
    ONLINE, OFFLINE,
    MESSAGE, UPD_MESSAGE, READ, UNREAD_MESSAGES, BIND, REMOVE,
    ERROR
}
