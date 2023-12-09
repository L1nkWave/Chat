package com.chat.wsserver.websocket.jwt;

import java.security.Principal;

public record UserPrincipal(String jwt) implements Principal {

    public static final String JWT_HEADER_KEY = "Access-JWT";

    @Override
    public String getName() {
        return String.format("user[%s]", jwt.substring(0, 5));
    }

}
