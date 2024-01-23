package com.chat.wsserver.websocket.jwt;

import org.springframework.lang.NonNull;

import java.security.Principal;

public record UserPrincipal(String rawAccessToken, @NonNull Token token) implements Principal {

    @Override
    public String getName() {
        return token.username();
    }

}
