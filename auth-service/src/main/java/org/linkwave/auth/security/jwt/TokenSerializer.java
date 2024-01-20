package org.linkwave.auth.security.jwt;

import org.springframework.lang.NonNull;

public interface TokenSerializer {
    String serialize(@NonNull Token token);
}
