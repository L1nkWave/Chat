package com.chat.wsserver.websocket.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Component
public class JwtAccessParser implements TokenParser {

    private static final String PUBLIC_KEY_PATH = "classpath:keys/access_public_key.pem";

    private final Algorithm algorithm;

    public JwtAccessParser(@NonNull ResourceLoader loader) throws IOException {
        try (var resource = loader.getResource(PUBLIC_KEY_PATH).getInputStream()) {
            final RSAPublicKey publicKey = RsaKeyConverters.x509().convert(resource);
            this.algorithm = Algorithm.RSA256(publicKey, null);
        }
    }

    @Override
    public Token parse(String token) {
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

}
