package org.linkwave.auth.security;

import lombok.RequiredArgsConstructor;
import org.linkwave.auth.entity.Role;
import org.linkwave.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).map(user ->
                new DefaultUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.isDeleted(),
                        user.isBlocked(),
                        user.getRoles().stream()
                                .map(Role::getName)
                                .map(SimpleGrantedAuthority::new)
                                .toList())
        ).orElseThrow(() -> new UsernameNotFoundException("username not found"));
    }

}
