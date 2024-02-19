package org.linkwave.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linkwave.shared.auth.Token;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.linkwave.auth.dto.TokensDto;
import org.linkwave.auth.entity.DeactivatedToken;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.security.jwt.*;
import org.linkwave.shared.auth.JwtAccessSerializer;
import org.linkwave.shared.dto.ApiError;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.linkwave.auth.security.utils.Cookies.REFRESH_TOKEN;
import static org.linkwave.auth.security.utils.Cookies.createRefreshCookie;
import static org.linkwave.auth.security.utils.TokenAuthorities.JWT_REFRESH;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

public class JwtTokensRefreshFilter extends OncePerRequestFilter {

    private final RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/v1/auth/refresh-tokens", HttpMethod.POST.name());
    private final RefreshTokenFactory refreshTokenFactory = new RefreshTokenFactoryImpl();
    private final AccessTokenFactory accessTokenFactory = new AccessTokenFactoryImpl();

    private final JwtRefreshSerializer jwtRefreshSerializer;
    private final JwtAccessSerializer jwtAccessSerializer;
    private final JwtRefreshParser jwtRefreshParser;
    private final DeactivatedTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final UserDetailsService userDetailsService;

    public JwtTokensRefreshFilter(JwtRefreshSerializer jwtRefreshSerializer, JwtAccessSerializer jwtAccessSerializer,
                                  JwtRefreshParser jwtRefreshParser, DeactivatedTokenRepository tokenRepository,
                                  ObjectMapper objectMapper, UserDetailsService userDetailsService) {
        this.jwtRefreshSerializer = jwtRefreshSerializer;
        this.jwtAccessSerializer = jwtAccessSerializer;
        this.jwtRefreshParser = jwtRefreshParser;
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final Optional<Cookie> optionalCookie = Stream.of(request.getCookies() != null
                ? request.getCookies() : new Cookie[0])
                .filter(cookie -> cookie.getName().equals(REFRESH_TOKEN))
                .findAny();

        try {

            if (optionalCookie.isEmpty()) {
                throw new CredentialsExpiredException("Refresh token is not present");
            }

            final String refreshToken = optionalCookie.get().getValue();
            final Token token = jwtRefreshParser.parse(refreshToken);

            if (token == null || tokenRepository.existsById(token.id())) {
                throw new BadCredentialsException("Refresh token is unavailable");
            }

            if (!token.authorities().contains(JWT_REFRESH)) {
                throw new AccessDeniedException("You do not have permissions");
            }

            // mark existing tokens as deactivated
            tokenRepository.save(new DeactivatedToken(token.id(), token.expireAt()));

            // find user by username
            final var userDetails = userDetailsService.loadUserByUsername(token.username());

            // create a new pair of tokens
            final Token newRefreshToken = refreshTokenFactory.refreshWith(token, userDetails.getAuthorities());
            final Token accessToken = accessTokenFactory.build(newRefreshToken);
            final String serializedRefreshToken = jwtRefreshSerializer.serialize(newRefreshToken);

            // send refresh token as cookie
            final var cookie = createRefreshCookie(
                    serializedRefreshToken,
                    Duration.between(newRefreshToken.createdAt(), newRefreshToken.expireAt())
            );

            response.setHeader(SET_COOKIE, cookie.toString());
            response.setStatus(SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            final var tokensDto = new TokensDto(newRefreshToken.expireAt(), jwtAccessSerializer.serialize(accessToken));
            objectMapper.writeValue(response.getOutputStream(), tokensDto);

        } catch (CredentialsExpiredException | AccessDeniedException |
                 BadCredentialsException | UsernameNotFoundException e) {

            int status;
            if (e instanceof CredentialsExpiredException) {
                status = SC_UNAUTHORIZED;
            } else if (e instanceof AccessDeniedException) {
                status = SC_FORBIDDEN;
            } else {
                status = SC_BAD_REQUEST;
            }
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(status);
            objectMapper.writeValue(response.getOutputStream(), ApiError.builder()
                    .path(request.getRequestURI())
                    .timestamp(Instant.now())
                    .message(e.getMessage())
                    .status(status)
                    .build());
        } finally {
            var os = response.getOutputStream();
            os.flush();
            os.close();
        }
    }

}
