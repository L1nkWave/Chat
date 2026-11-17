package org.linkwave.auth.security;

import lombok.RequiredArgsConstructor;
import org.linkwave.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

/**
 * Bọc AuthenticationManager gốc: chặn đăng nhập khi tài khoản đang bị khóa tạm,
 * và ghi nhận số lần sai.
 *
 * Tài khoản bị khóa/xóa (DisabledException) chỉ được báo rõ khi mật khẩu đúng,
 * để người không biết mật khẩu không dò được trạng thái tài khoản.
 */
@RequiredArgsConstructor
public class ThrottledAuthenticationManager implements AuthenticationManager {

    private final AuthenticationManager delegate;
    private final LoginAttemptService attempts;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getName();

        final Duration locked = attempts.lockedFor(username);
        if (!locked.isZero()) {
            final long minutes = Math.max(1, (locked.toSeconds() + 59) / 60);
            throw new LockedException("Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút.");
        }

        try {
            final Authentication result = delegate.authenticate(authentication);
            attempts.reset(username);
            return result;
        } catch (BadCredentialsException e) {
            attempts.recordFailure(username);
            throw e;
        } catch (DisabledException e) {
            // Tài khoản bị khóa/xóa: chỉ báo rõ khi mật khẩu đúng
            if (passwordMatches(username, authentication)) {
                throw e;
            }
            attempts.recordFailure(username);
            throw new BadCredentialsException("Incorrect username or password", e);
        }
    }

    private boolean passwordMatches(String username, Authentication authentication) {
        final Object credentials = authentication.getCredentials();
        if (credentials == null) {
            return false;
        }
        return userRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(credentials.toString(), user.getPassword()))
                .orElse(false);
    }
}