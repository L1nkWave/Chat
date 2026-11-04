package org.linkwave.auth.security.filter;

import tools.jackson.databind.ObjectMapper;
import org.linkwave.shared.auth.Token;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import org.linkwave.auth.dto.TokensDto;
import org.linkwave.auth.security.CredentialsAuthenticationConverter;
import org.linkwave.auth.security.UnAuthorizedAuthenticationEntryPoint;
import org.linkwave.auth.security.jwt.*;
import org.linkwave.shared.auth.JwtAccessSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.linkwave.auth.security.utils.Cookies.createRefreshCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

public class JwtTokensInitializerFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(JwtTokensInitializerFilter.class);

    private final RequestMatcher requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login");
    private final RefreshTokenFactory refreshTokenFactory = new RefreshTokenFactoryImpl();
    private final AccessTokenFactory accessTokenFactory = new AccessTokenFactoryImpl();

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationManager authenticationManager;
    private final JwtRefreshSerializer jwtRefreshSerializer;
    private final JwtAccessSerializer jwtAccessSerializer;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectMapper objectMapper;


    public JwtTokensInitializerFilter(ObjectMapper objectMapper,
                                      AuthenticationManager authenticationManager,
                                      JwtRefreshSerializer jwtRefreshSerializer,
                                      JwtAccessSerializer jwtAccessSerializer) {
        this.objectMapper = objectMapper;
        this.authenticationConverter = new CredentialsAuthenticationConverter(objectMapper);
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
        this.authenticationManager = authenticationManager;
        this.jwtRefreshSerializer = jwtRefreshSerializer;
        this.jwtAccessSerializer = jwtAccessSerializer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.debug("-> doFilterInternal():");

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = authenticationConverter.convert(request);
        if (authentication == null) {
            log.debug("-> doFilterInternal(): authentication is null");
            authenticationEntryPoint.commence(
                    request, response,
                    new BadCredentialsException("Incorrect username or password")
            );
            return;
        }

        try {
            authentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            log.debug("-> doFilterInternal(): authentication failed");
            authenticationEntryPoint.commence(
                    request, response,
                    new BadCredentialsException("Incorrect username or password")
            );
            return;
        }

        final Token refreshToken = refreshTokenFactory.build(authentication);
        final Token accessToken = accessTokenFactory.build(refreshToken);
        String serializedRefreshToken = jwtRefreshSerializer.serialize(refreshToken);

        // send refresh token as cookie
        final var cookie = createRefreshCookie(
                serializedRefreshToken,
                Duration.between(refreshToken.createdAt(), refreshToken.expireAt())
        );

        response.setHeader(SET_COOKIE, cookie.toString());
        response.setStatus(SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        @Cleanup final var outputStream = response.getOutputStream();
        final var tokensDto = new TokensDto(refreshToken.expireAt(), jwtAccessSerializer.serialize(accessToken));
        objectMapper.writeValue(outputStream, tokensDto);
        outputStream.flush();
    }
}
