package org.linkwave.auth.security.jwt;

import org.springframework.lang.NonNull;

public interface AccessTokenFactory {
    Token build(@NonNull Token refreshToken);
}
