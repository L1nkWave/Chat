package org.linkwave.chatservice.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@UtilityClass
public class RequestUtils {

    public static DefaultUserDetails userDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @NonNull
    public static RequestInitiator requestInitiator(@NonNull HttpServletRequest request) {
        return new RequestInitiator(userDetails().id(), request.getHeader(AUTHORIZATION));
    }

}
