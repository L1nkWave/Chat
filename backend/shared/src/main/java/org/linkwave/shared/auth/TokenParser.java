package org.linkwave.shared.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.UUID;

public interface TokenParser {

    Token parse(String token);

    default Token parse(String token, Algorithm algorithm) {
        final JWTVerifier verifier = JWT.require(algorithm).build();
        try {
            final DecodedJWT jwt = verifier.verify(token);
            return Token.builder()
                    .id(UUID.fromString(jwt.getClaim("token-id").asString()))
                    .userId(jwt.getClaim("user-id").asLong())
                    .username(jwt.getSubject())
                    .authorities(jwt.getClaim("authorities").asList(String.class))
                    .createdAt(jwt.getIssuedAtAsInstant())
                    .expireAt(jwt.getExpiresAtAsInstant())
                    .build();
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    default boolean isInvalid(String token) {
        return parse(token) == null;
    }

}
