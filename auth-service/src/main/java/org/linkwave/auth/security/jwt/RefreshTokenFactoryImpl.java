package org.linkwave.auth.security.jwt;

import org.linkwave.auth.security.DefaultUserDetails;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.linkwave.auth.security.utils.TokenAuthorities.JWT_REFRESH;

public class RefreshTokenFactoryImpl implements RefreshTokenFactory {

    private final Duration duration = Duration.ofHours(1);

    @Override
    public Token build(@NonNull Authentication authentication) {
        final var userDetails = (DefaultUserDetails) authentication.getPrincipal();
        final var now = Instant.now();

        return Token.builder()
                .id(UUID.randomUUID())
                .createdAt(now)
                .expireAt(now.plus(duration))
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .authorities(prepareAuthorities(userDetails.getAuthorities()))
                .build();
    }

    @Override
    public Token refreshWith(@NonNull Token existingToken, @NonNull Collection<? extends GrantedAuthority> authorities) {
        final var now = Instant.now();

        return Token.builder()
                .id(UUID.randomUUID())
                .userId(existingToken.userId())
                .username(existingToken.username())
                .createdAt(now)
                .authorities(prepareAuthorities(authorities))
                .expireAt(now.plus(duration))
                .build();
    }

    private List<String> prepareAuthorities(@NonNull Collection<? extends GrantedAuthority> authorities) {
        final List<GrantedAuthority> newAuthorities = new LinkedList<>(authorities);
        newAuthorities.add(new SimpleGrantedAuthority(JWT_REFRESH));

        return newAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

}
