package org.linkwave.ws.websocket.jwt;

import org.springframework.lang.NonNull;

import org.linkwave.shared.auth.Token;

import java.security.Principal;

public record UserPrincipal(String rawAccessToken, @NonNull Token token) implements Principal {

    @Override
    public String getName() {
        return token.username();
    }

}
