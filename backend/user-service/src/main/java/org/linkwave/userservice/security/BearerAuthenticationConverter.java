package org.linkwave.userservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.security.interfaces.RSAPublicKey;

@RequiredArgsConstructor
public class BearerAuthenticationConverter implements AuthenticationConverter {

    public static final int TOKEN_START_POSITION = 7;
    public static final String BEARER_PREFIX = "Bearer ";

    private final Algorithm algorithm;

    public BearerAuthenticationConverter(RSAPublicKey publicKey) {
        this.algorithm = Algorithm.RSA256(publicKey, null);
    }

    @Override
    public Authentication convert(@NonNull HttpServletRequest request) {
        final var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        try {
            final var verifier = JWT.require(algorithm).build();
            final var decodedJWT = verifier.verify(authHeader.substring(TOKEN_START_POSITION));

            final var authorities = decodedJWT.getClaim("authorities")
                    .asList(String.class)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            final var userDetails = DefaultUserDetails.builder()
                    .id(decodedJWT.getClaim("user-id").asLong())
                    .username(decodedJWT.getSubject())
                    .authorities(authorities)
                    .build();

            return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, authorities);

        } catch (JWTVerificationException e) {
            return null;
        }
    }

}