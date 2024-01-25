package com.chat.wsserver.websocket.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Component
public class JwtAccessParser implements TokenParser {

    private static final String PRIVATE_KEY_PATH = "classpath:keys/access_public_key.pem";

    private final Algorithm algorithm;

    public JwtAccessParser() throws IOException {
        final var resource = ResourceUtils.getFile(PRIVATE_KEY_PATH).toPath();
        final RSAPublicKey publicKey = RsaKeyConverters.x509()
                .convert(new ByteArrayInputStream(Files.readAllBytes(resource)));
        this.algorithm = Algorithm.RSA256(publicKey, null);
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
