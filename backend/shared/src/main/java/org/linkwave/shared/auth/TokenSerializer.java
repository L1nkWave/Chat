package org.linkwave.shared.auth;

import org.springframework.lang.NonNull;

public interface TokenSerializer {
    String serialize(@NonNull Token token);
}
