package org.linkwave.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.linkwave.auth.entity.DeactivatedToken;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.security.UnAuthorizedAuthenticationEntryPoint;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.Token;
import org.linkwave.auth.security.utils.Cookies;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtLogoutFilter extends OncePerRequestFilter {

    private final RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/v1/auth/logout", HttpMethod.POST.name());
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessParser jwtAccessParser;
    private final DeactivatedTokenRepository tokenRepository;

    public JwtLogoutFilter(ObjectMapper objectMapper,
                           JwtAccessParser jwtAccessParser,
                           DeactivatedTokenRepository tokenRepository) {
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
        this.jwtAccessParser = jwtAccessParser;
        this.tokenRepository = tokenRepository;
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

            // mark tokens as deactivated by their id
            tokenRepository.save(new DeactivatedToken(accessToken.id(), accessToken.expireAt()));

            // clear cookie
            final var cookie = Cookies.createRefreshCookie("", -1);
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

}
