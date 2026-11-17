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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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

    @Transactional
    public void logoutAllDevices(Long userId) {
        final User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setTokensValidAfter(revocationMark());
    }

    @Transactional(readOnly = true)
    public boolean isRevokedSince(Long userId, Instant issuedAt) {
        final User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return true;
        }
        final Instant validAfter = user.getTokensValidAfter();
        return validAfter != null && issuedAt.isBefore(validAfter);
    }

    @Transactional(readOnly = true)
    public boolean requiresPasswordChange(Long userId) {
        return userRepository.findById(userId).map(User::isMustChangePassword).orElse(false);
    }

    /**
     * Đổi mật khẩu: kiểm tra chính sách mật khẩu mới, xác nhận mật khẩu cũ, rồi ghi mật khẩu mới.
     * Mọi phiên cũ bị thu hồi (qua mốc tokensValidAfter), người dùng phải đăng nhập lại.
     */
    @Transactional
    public void changePassword(Long userId, String oldRawPassword, String newRawPassword) {
        final User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        validateNewPassword(user.getUsername(), newRawPassword);

        if (!passwordEncoder.matches(oldRawPassword, user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu cũ không đúng");
        }
        if (passwordEncoder.matches(newRawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(newRawPassword));
        user.setMustChangePassword(false);
        user.setTokensValidAfter(revocationMark());
    }

    /** Mốc thu hồi làm tròn lên một giây: iat chỉ có độ chính xác đến giây nên refresh token cấp trong cùng giây trước đó vẫn bị thu hồi. */
    private static Instant revocationMark() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    }

    private static void validateNewPassword(String username, String newPassword) {
        if (newPassword.length() < 8 || newPassword.length() > 64) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 64 ký tự");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có cả chữ và số");
        }
        if (username != null && newPassword.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng tên đăng nhập");
        }
    }

}