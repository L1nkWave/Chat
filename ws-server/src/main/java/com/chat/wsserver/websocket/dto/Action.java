package com.chat.wsserver.websocket.dto;

public enum Action {
    JOIN, LEAVE,
    ONLINE, OFFLINE,
    MESSAGE, UPD_MESSAGE,
    ERROR
}
