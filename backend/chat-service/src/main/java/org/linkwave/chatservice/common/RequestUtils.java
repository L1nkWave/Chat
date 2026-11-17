package org.linkwave.chatservice.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

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

    /**
     * ADMIN/ROOT phải có cờ MFA_VERIFIED trong token (đã bật và xác thực 2FA).
     * Gọi ở mọi endpoint quản trị của chat-service để không phụ thuộc user-service.
     */
    public static void requireMfaIfPrivileged() {
        final List<String> authorities = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new PrivacyViolationException("Cần bật và xác thực hai lớp trước khi dùng chức năng quản trị");
        }
    }

}