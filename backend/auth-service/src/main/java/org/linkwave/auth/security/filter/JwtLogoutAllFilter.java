package org.linkwave.auth.security.filter;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.security.UnAuthorizedAuthenticationEntryPoint;
import org.linkwave.auth.security.utils.Cookies;
import org.linkwave.auth.service.UserService;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.Token;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Đăng xuất tất cả thiết bị: đánh dấu mốc thu hồi cho tài khoản (mọi refresh token cũ bị từ chối),
 * vô hiệu hóa access token hiện tại ngay lập tức và xóa cookie refresh của trình duyệt này.
 * Access token của thiết bị khác tối đa còn hiệu lực đến khi hết hạn (5 phút).
 */
public class JwtLogoutAllFilter extends OncePerRequestFilter {

    private final RequestMatcher requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/logout-all");
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessParser jwtAccessParser;
    private final DeactivatedTokenRepository tokenRepository;
    private final UserService userService;

    public JwtLogoutAllFilter(ObjectMapper objectMapper,
                              JwtAccessParser jwtAccessParser,
                              DeactivatedTokenRepository tokenRepository,
                              UserService userService) {
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
        this.jwtAccessParser = jwtAccessParser;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
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

            // vô hiệu hóa access token hiện tại; nếu yêu cầu khác vừa dùng token này thì từ chối
            if (tokenRepository.insertIfAbsent(accessToken.id(), accessToken.expireAt()) == 0) {
                throw new CredentialsExpiredException("Access token is unavailable");
            }

            // thu hồi mọi refresh token đã cấp cho tài khoản
            userService.logoutAllDevices(accessToken.userId());

            // clear cookie
            final var cookie = Cookies.createRefreshCookie("", -1);
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

}