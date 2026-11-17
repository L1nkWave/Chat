package org.linkwave.auth.security.filter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.security.LoginAttemptService;
import org.linkwave.auth.security.UnAuthorizedAuthenticationEntryPoint;
import org.linkwave.auth.security.utils.Cookies;
import org.linkwave.auth.service.UserService;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.Token;
import org.linkwave.shared.dto.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Đổi mật khẩu: POST /api/v1/auth/change-password với body {oldPassword, newPassword}.
 * Yêu cầu access token hợp lệ. Sai mật khẩu cũ được tính vào giới hạn đăng nhập sai.
 * Thành công: thu hồi mọi phiên (kể cả phiên hiện tại) và xóa cookie refresh, người dùng đăng nhập lại.
 */
public class JwtChangePasswordFilter extends OncePerRequestFilter {

    public record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    private final RequestMatcher requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/change-password");
    private final ObjectMapper objectMapper;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessParser jwtAccessParser;
    private final DeactivatedTokenRepository tokenRepository;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public JwtChangePasswordFilter(ObjectMapper objectMapper,
                                   JwtAccessParser jwtAccessParser,
                                   DeactivatedTokenRepository tokenRepository,
                                   UserService userService,
                                   LoginAttemptService loginAttemptService) {
        this.objectMapper = objectMapper;
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
        this.jwtAccessParser = jwtAccessParser;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {

            if (header == null || !header.startsWith("Bearer ")) {
                throw new BadCredentialsException("Bearer is not present");
            }

            final Token accessToken = jwtAccessParser.parse(header.substring(7));
            if (accessToken == null || tokenRepository.existsById(accessToken.id())) {
                throw new CredentialsExpiredException("Access token is unavailable");
            }

            final ChangePasswordRequest body = readBody(request);
            final String username = accessToken.username();

            if (!loginAttemptService.lockedFor(username).isZero()) {
                writeError(request, response, HttpStatus.TOO_MANY_REQUESTS,
                        "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau.");
                return;
            }

            try {
                userService.changePassword(accessToken.userId(), body.oldPassword(), body.newPassword());
            } catch (BadCredentialsException e) {
                loginAttemptService.recordFailure(username);
                writeError(request, response, HttpStatus.BAD_REQUEST, e.getMessage());
                return;
            } catch (IllegalArgumentException e) {
                writeError(request, response, HttpStatus.BAD_REQUEST, e.getMessage());
                return;
            }
            loginAttemptService.reset(username);

            // vô hiệu hóa access token hiện tại; nếu yêu cầu khác vừa dùng token này thì từ chối
            if (tokenRepository.insertIfAbsent(accessToken.id(), accessToken.expireAt()) == 0) {
                throw new CredentialsExpiredException("Access token is unavailable");
            }

            // xóa cookie refresh của trình duyệt này
            final var cookie = Cookies.createRefreshCookie("", -1);
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (IllegalArgumentException e) {
            writeError(request, response, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

    private ChangePasswordRequest readBody(HttpServletRequest request) {
        try {
            final ChangePasswordRequest body = objectMapper.readValue(request.getInputStream(), ChangePasswordRequest.class);
            if (body == null || body.oldPassword() == null || body.newPassword() == null) {
                throw new IllegalArgumentException("Thiếu mật khẩu cũ hoặc mật khẩu mới");
            }
            return body;
        } catch (IOException | JacksonException e) {
            throw new IllegalArgumentException("Nội dung yêu cầu không hợp lệ");
        }
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.builder()
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .status(status.value())
                .message(message)
                .build());
    }

}