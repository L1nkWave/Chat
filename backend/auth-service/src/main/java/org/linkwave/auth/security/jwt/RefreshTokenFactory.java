package org.linkwave.auth.security.jwt;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.linkwave.shared.auth.Token;

import java.util.Collection;

public interface RefreshTokenFactory {
    Token build(@NonNull Authentication authentication);
    Token refreshWith(@NonNull Token existingToken, @NonNull Collection<? extends GrantedAuthority> authorities);
}
