package org.linkwave.shared.utils;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

@UtilityClass
public class Bearers {

    public static final int TOKEN_START_POSITION = 7;
    public static final String BEARER_PREFIX = "Bearer ";

    public static String extract(@NonNull String bearer) {
        return bearer.substring(TOKEN_START_POSITION);
    }

    public static String append(@NonNull String token) {
        return "%s%s".formatted(BEARER_PREFIX, token);
    }

}
