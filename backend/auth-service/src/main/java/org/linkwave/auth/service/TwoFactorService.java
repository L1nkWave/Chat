package org.linkwave.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.auth.entity.AuditLog;
import org.linkwave.auth.entity.TotpRecoveryCode;
import org.linkwave.auth.entity.TotpSecret;
import org.linkwave.auth.repository.AuditLogRepository;
import org.linkwave.auth.repository.TotpRecoveryCodeRepository;
import org.linkwave.auth.repository.TotpSecretRepository;
import org.linkwave.auth.security.totp.TotpAlgorithm;
import org.linkwave.auth.security.totp.TotpCrypto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MINUTES = 5;
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TotpSecretRepository secrets;
    private final TotpRecoveryCodeRepository recoveryCodes;
    private final AuditLogRepository auditLogs;
    private final TotpCrypto crypto;
    // Không inject PasswordEncoder từ SecurityConfig: tránh vòng phụ thuộc bean (SecurityConfig -> filter -> service -> SecurityConfig)
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public enum Check { OK, INVALID, LOCKED }

    public record SetupResponse(String secret, String otpauthUri) {
    }

    /** Bắt buộc 2FA với ADMIN và ROOT khi đã bật. */
    public boolean isRequired(Long userId, Collection<? extends GrantedAuthority> authorities) {
        final boolean privileged = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_ROOT"));
        return privileged && secrets.findById(userId).map(TotpSecret::isEnabled).orElse(false);
    }

    @Transactional
    public SetupResponse setup(Long userId, String username) {
        final TotpSecret existing = secrets.findById(userId).orElse(null);
        if (existing != null && existing.isEnabled()) {
            throw new ResponseStatusException(CONFLICT, "2FA đã được bật cho tài khoản này");
        }
        final String secret = TotpAlgorithm.newSecretBase32();
        secrets.save(TotpSecret.builder()
                .userId(userId)
                .secretEncrypted(crypto.encrypt(TotpAlgorithm.base32Decode(secret)))
                .keyVersion(crypto.currentVersion())
                .build());
        final String label = URLEncoder.encode("SGU Chat:" + username, StandardCharsets.UTF_8).replace("+", "%20");
        final String issuer = URLEncoder.encode("SGU Chat", StandardCharsets.UTF_8).replace("+", "%20");
        return new SetupResponse(secret,
                "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30");
    }

    /** Xác nhận bằng mã TOTP đầu tiên. Trả về 8 mã khôi phục (chỉ hiện một lần). */
    @Transactional
    public List<String> confirm(Long userId, String code) {
        final TotpSecret secret = secrets.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Chưa khởi tạo 2FA"));
        if (secret.isEnabled()) {
            throw new ResponseStatusException(CONFLICT, "2FA đã được bật");
        }
        final long step = matchStep(secret, normalize(code));
        if (step < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Mã xác thực không đúng");
        }
        secret.setEnabled(true);
        secret.setEnabledAt(ZonedDateTime.now());
        secret.setLastUsedStep(step);
        secrets.save(secret);

        recoveryCodes.deleteByUserId(userId);
        final List<String> plain = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            final String display = randomCode();
            plain.add(display.substring(0, 5) + "-" + display.substring(5));
            recoveryCodes.save(TotpRecoveryCode.builder()
                    .userId(userId)
                    .codeHash(passwordEncoder.encode(display))
                    .build());
        }
        audit(userId, "2FA_ENABLE");
        return plain;
    }

    /** Dùng khi đăng nhập. Không ném lỗi để bộ đếm thử sai được lưu. Controller sẽ báo lỗi. */
    @Transactional
    public Check verifyLogin(Long userId, String rawCode) {
        final TotpSecret secret = secrets.findById(userId).filter(TotpSecret::isEnabled).orElse(null);
        if (secret == null) {
            return Check.INVALID;
        }
        if (secret.getLockedUntil() != null && secret.getLockedUntil().isAfter(ZonedDateTime.now())) {
            audit(userId, "2FA_LOCKED_ATTEMPT");
            return Check.LOCKED;
        }

        final String code = normalize(rawCode);
        boolean ok;
        if (code.length() == 6 && code.chars().allMatch(Character::isDigit)) {
            final long step = matchStep(secret, code);
            ok = step >= 0;
            if (ok) {
                secret.setLastUsedStep(step);
            }
        } else {
            ok = consumeRecoveryCode(userId, code);
        }

        if (ok) {
            secret.setFailedAttempts(0);
            secret.setLockedUntil(null);
            secrets.save(secret);
            audit(userId, "2FA_LOGIN_OK");
            return Check.OK;
        }

        secret.setFailedAttempts(secret.getFailedAttempts() + 1);
        if (secret.getFailedAttempts() >= MAX_FAILURES) {
            secret.setFailedAttempts(0);
            secret.setLockedUntil(ZonedDateTime.now().plusMinutes(LOCK_MINUTES));
            audit(userId, "2FA_LOCKED");
        }
        secrets.save(secret);
        audit(userId, "2FA_LOGIN_FAIL");
        return Check.INVALID;
    }

    /** Tắt 2FA sau khi xác nhận bằng mã hiện tại. */
    @Transactional
    public Check disable(Long userId, String rawCode) {
        final Check check = verifyLogin(userId, rawCode);
        if (check == Check.OK) {
            secrets.deleteById(userId);
            recoveryCodes.deleteByUserId(userId);
            audit(userId, "2FA_DISABLE");
        }
        return check;
    }

    private long matchStep(TotpSecret secret, String code) {
        final byte[] key = crypto.decrypt(secret.getSecretEncrypted(), secret.getKeyVersion());
        final long now = TotpAlgorithm.currentStep(Instant.now().getEpochSecond());
        return TotpAlgorithm.matchStep(key, code, now, secret.getLastUsedStep());
    }

    private boolean consumeRecoveryCode(Long userId, String code) {
        for (TotpRecoveryCode candidate : recoveryCodes.findByUserIdAndUsedAtIsNull(userId)) {
            if (passwordEncoder.matches(code, candidate.getCodeHash())) {
                candidate.setUsedAt(ZonedDateTime.now());
                recoveryCodes.save(candidate);
                return true;
            }
        }
        return false;
    }

    private void audit(Long userId, String action) {
        auditLogs.save(AuditLog.builder()
                .actorId(userId)
                .action(action)
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .build());
    }

    /** Bỏ khoảng trắng và dấu gạch, chuyển chữ hoa. Dùng cho cả mã TOTP và mã khôi phục. */
    static String normalize(String raw) {
        return raw == null ? "" : raw.replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    private static String randomCode() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(RECOVERY_ALPHABET.charAt(RANDOM.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }

}