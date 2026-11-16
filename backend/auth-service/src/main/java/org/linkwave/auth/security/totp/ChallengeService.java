package org.linkwave.auth.security.totp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Challenge ngắn hạn sau khi đã đúng mật khẩu nhưng chưa nhập mã 2FA.
 * Không phải JWT: gateway và các service không chấp nhận token này như access token.
 * Định dạng: base64url(userId.exp) + "." + base64url(HMAC-SHA256). Khóa lấy từ TWO_FACTOR_CHALLENGE_SECRET.
 */
@Component
public class ChallengeService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final byte[] secret;

    public ChallengeService(@Value("${TWO_FACTOR_CHALLENGE_SECRET}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException("TWO_FACTOR_CHALLENGE_SECRET phải dài tối thiểu 32 ký tự");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long userId) {
        final String payload = userId + "." + Instant.now().plus(TTL).getEpochSecond();
        return encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
    }

    public Long verify(String token) {
        final String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw invalid();
        }
        final byte[] payload = decode(parts[0]);
        final byte[] signature = decode(parts[1]);
        if (!MessageDigest.isEqual(sign(new String(payload, StandardCharsets.UTF_8)), signature)) {
            throw invalid();
        }
        final String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\.");
        final long expiresAt = Long.parseLong(fields[1]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new ResponseStatusException(UNAUTHORIZED, "Phiên xác thực 2 lớp đã hết hạn, hãy đăng nhập lại");
        }
        return Long.parseLong(fields[0]);
    }

    private byte[] sign(String payload) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 không khả dụng", e);
        }
    }

    private static String encode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] decode(String data) {
        try {
            return Base64.getUrlDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(UNAUTHORIZED, "Phiên xác thực 2 lớp không hợp lệ");
    }

}