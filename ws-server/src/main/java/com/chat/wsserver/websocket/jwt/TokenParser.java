package com.chat.wsserver.websocket.jwt;

public interface TokenParser {

    Token parse(String token);

    default boolean isInvalid(String token) {
        return parse(token) == null;
    }

}
