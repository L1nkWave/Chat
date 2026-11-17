package org.linkwave.auth.security.utils;

import org.linkwave.shared.auth.Token;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TokenAuthorities {
    public static final String AUTHORITY_PREFIX = "GRANT_";
    public static final String JWT_REFRESH = "GRANT_JWT_REFRESH";

    /** Đã xác thực đủ: đăng nhập thường, hoặc đã nhập mã 2FA. ADMIN/ROOT bắt buộc có cờ này. */
    public static final String MFA_VERIFIED = "MFA_VERIFIED";

    /** ADMIN hoặc ROOT: vai trò bắt buộc bật 2FA. */
    public static boolean isPrivileged(Collection<String> authorities) {
        return authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
    }

    /** Trả về token mới với cờ MFA_VERIFIED được bật hoặc tắt theo tham số. */
    public static Token withMfa(Token token, boolean verified) {
        final List<String> authorities = new ArrayList<>(token.authorities());
        authorities.remove(MFA_VERIFIED);
        if (verified) {
            authorities.add(MFA_VERIFIED);
        }
        return Token.builder()
                .id(token.id())
                .userId(token.userId())
                .username(token.username())
                .createdAt(token.createdAt())
                .expireAt(token.expireAt())
                .authorities(authorities)
                .build();
    }
}