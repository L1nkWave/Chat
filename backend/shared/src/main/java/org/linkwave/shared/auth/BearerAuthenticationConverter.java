package org.linkwave.shared.auth;

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

import static org.linkwave.shared.utils.Bearers.*;

@RequiredArgsConstructor
public class BearerAuthenticationConverter implements AuthenticationConverter {

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
            final var decodedJWT = verifier.verify(extract(authHeader));

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
