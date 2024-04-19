package org.linkwave.ws.websocket.dto;

public enum Action {

    // Chat
    JOIN, LEAVE, ADD, KICK, SET_ROLE,

    // User status
    ONLINE, OFFLINE,

    // Messages
    MESSAGE, BIND, UPD_MESSAGE,
    READ, UNREAD_MESSAGES,
    REMOVE, CLEAR_HISTORY,

    // Error
    ERROR
}
