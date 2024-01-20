package org.linkwave.auth.security.jwt;

import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.linkwave.auth.security.utils.TokenAuthorities.AUTHORITY_PREFIX;

public class AccessTokenFactoryImpl implements AccessTokenFactory {

    private final Duration duration = Duration.ofMinutes(10);

    @Override
    public Token build(@NonNull Token refreshToken) {
        return Token.builder()
                .id(refreshToken.id())
                .userId(refreshToken.userId())
                .username(refreshToken.username())
                .createdAt(refreshToken.createdAt())
                .expireAt(refreshToken.createdAt().plus(duration))
                .authorities(refreshToken.authorities().stream()
                        .filter(authority -> !authority.startsWith(AUTHORITY_PREFIX))
                        .collect(Collectors.toList()))
                .build();
    }

}
