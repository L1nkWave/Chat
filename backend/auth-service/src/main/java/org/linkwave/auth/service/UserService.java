package org.linkwave.auth.service;

import lombok.RequiredArgsConstructor;
import org.linkwave.auth.entity.DeactivatedToken;
import org.linkwave.auth.entity.User;
import org.linkwave.auth.exception.UserNotFoundException;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.repository.UserRepository;
import org.linkwave.shared.auth.Token;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final DeactivatedTokenRepository deactivatedTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void deleteAccount(Token accessToken, String userPassword) {
        if (accessToken == null || deactivatedTokenRepository.existsById(accessToken.id())) {
            throw new CredentialsExpiredException("Access token is unavailable");
        }

        final User user = userRepository.findById(accessToken.userId()).orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(userPassword, user.getPassword())) {
            throw new BadCredentialsException("Credentials not valid");
        }

        final var deactivatedToken = new DeactivatedToken(accessToken.id(), accessToken.expireAt());
        deactivatedTokenRepository.save(deactivatedToken);

        user.setDeleted(true);
        user.setUsername(null);
        user.setOnline(false);
        user.setLastSeen(ZonedDateTime.now());
    }

}
