package org.linkwave.ws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Chỉ cho phép chat-service (qua khóa nội bộ) gọi /api/v1/ws-chats/**.
 * Người dùng đã đăng nhập vẫn bị chặn nếu không có khóa, nên không thể giả thông báo.
 */
@Component
public class InternalCallFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/api/v1/ws-chats";
    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final byte[] expectedKey;

    public InternalCallFilter(@Value("${internal.service-key}") String internalKey) {
        this.expectedKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        final String provided = request.getHeader(INTERNAL_KEY_HEADER);
        if (provided == null || !MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ dịch vụ nội bộ được gọi API này");
            return;
        }
        chain.doFilter(request, response);
    }
}