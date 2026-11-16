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
import java.time.Instant;
import org.linkwave.shared.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
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
import org.linkwave.auth.security.DefaultUserDetails;
import org.linkwave.auth.security.totp.ChallengeService;
import org.linkwave.auth.service.TwoFactorService;
import java.util.Map;

public class JwtTokensInitializerFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(JwtTokensInitializerFilter.class);

    private final RequestMatcher requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login");
    private final RefreshTokenFactory refreshTokenFactory = new RefreshTokenFactoryImpl();
    private final AccessTokenFactory accessTokenFactory = new AccessTokenFactoryImpl();

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationManager authenticationManager;
    private final JwtRefreshSerializer jwtRefreshSerializer;
    private final JwtAccessSerializer jwtAccessSerializer;
    private final TwoFactorService twoFactorService;
    private final ChallengeService challengeService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectMapper objectMapper;


    public JwtTokensInitializerFilter(ObjectMapper objectMapper,
                                      AuthenticationManager authenticationManager,
                                      JwtRefreshSerializer jwtRefreshSerializer,
                                      JwtAccessSerializer jwtAccessSerializer,
                                      TwoFactorService twoFactorService,
                                      ChallengeService challengeService) {
        this.objectMapper = objectMapper;
        this.authenticationConverter = new CredentialsAuthenticationConverter(objectMapper);
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
        this.authenticationManager = authenticationManager;
        this.jwtRefreshSerializer = jwtRefreshSerializer;
        this.jwtAccessSerializer = jwtAccessSerializer;
        this.twoFactorService = twoFactorService;
        this.challengeService = challengeService;
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
        } catch (LockedException e) {
            log.debug("-> doFilterInternal(): account temporarily locked");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiError.builder()
                    .path(request.getRequestURI())
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build());
            return;
        } catch (AuthenticationException e) {
            log.debug("-> doFilterInternal(): authentication failed");
            authenticationEntryPoint.commence(
                    request, response,
                    new BadCredentialsException("Incorrect username or password")
            );
            return;
        }

        // ADMIN/ROOT đã bật 2FA: chưa cấp token, chỉ trả về challenge ngắn hạn
        if (authentication.getPrincipal() instanceof DefaultUserDetails user
                && twoFactorService.isRequired(user.getId(), user.getAuthorities())) {
            log.debug("-> doFilterInternal(): two-factor required");
            response.setStatus(SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "twoFactorRequired", true,
                    "challenge", challengeService.issue(user.getId())));
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