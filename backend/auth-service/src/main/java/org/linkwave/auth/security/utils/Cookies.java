package org.linkwave.auth.security.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;

import java.time.Duration;

@UtilityClass
public class Cookies {
    public static final String REFRESH_TOKEN = "Refresh-Token";
    public static final String STRICT_SAME_SITE = "Strict";

    public static ResponseCookie createRefreshCookie(String value, @NonNull Duration ttl) {
        return createRefreshCookie(value, ttl.toSeconds());
    }

    public static ResponseCookie createRefreshCookie(String value, long ttlInSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN, value)
                .httpOnly(true)
                .sameSite(STRICT_SAME_SITE)
                .maxAge(ttlInSeconds)
                .path("/api/v1/auth/refresh-tokens")
                .build();
    }

}
