package org.linkwave.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class DefaultUserDetails extends User {

    private final Long id;
    private final boolean isDeleted;
    private final boolean isBlocked;

    public DefaultUserDetails(Long id, String username, String password, boolean isDeleted,  boolean isBlocked,
                              Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.isDeleted = isDeleted;
        this.isBlocked = isBlocked;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted && !isBlocked;
    }
}
