package org.linkwave.userservice.utils;

import lombok.experimental.UtilityClass;
import org.linkwave.auth.security.jwt.JwtAccessSerializer;
import org.linkwave.auth.security.jwt.Token;
import org.linkwave.auth.security.jwt.TokenSerializer;
import org.linkwave.userservice.entity.RoleEntity.Roles;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@UtilityClass
public class TokenGenerator {

    private static final String PUBLIC_KEY_PATH = CLASSPATH_URL_PREFIX + "/keys/access_private_key.pem";
    private static final String ISSUER = "LW-auth";

    private static final TokenSerializer TOKEN_SERIALIZER;

    static {
        final ResourceLoader resourceLoader = new DefaultResourceLoader(TokenGenerator.class.getClassLoader());
        try (var resource = resourceLoader.getResource(PUBLIC_KEY_PATH).getInputStream()) {
            final RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(resource);
            TOKEN_SERIALIZER = new JwtAccessSerializer(ISSUER, privateKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateToken(Long userId, String username, Duration ttl) {
        final var now = Instant.now();
        final var expireAt = now.plus(ttl);

        return TOKEN_SERIALIZER.serialize(Token.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .username(username)
                .authorities(List.of(Roles.USER.getValue()))
                .createdAt(now)
                .expireAt(expireAt)
                .build());
    }

}
