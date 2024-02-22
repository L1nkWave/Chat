package org.linkwave.auth.security.jwt;

import org.springframework.lang.NonNull;

import org.linkwave.shared.auth.Token;

public interface AccessTokenFactory {
    Token build(@NonNull Token refreshToken);
}
