package org.linkwave.ws.utils;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

@UtilityClass
public class RouteUtils {

    public static final String ROUTE_DELIMITER = "/";
    public static final String PATH_VAR_PREFIX = "{";
    public static final String PATH_VAR_POSTFIX = "}";

    public static boolean isPathVariable(@NonNull String s) {
        return s.startsWith(PATH_VAR_PREFIX) && s.endsWith(PATH_VAR_POSTFIX);
    }

    @NonNull
    public static String getPathVariable(@NonNull String s) {
        return s.substring(1, s.length() - 1);
    }

}
