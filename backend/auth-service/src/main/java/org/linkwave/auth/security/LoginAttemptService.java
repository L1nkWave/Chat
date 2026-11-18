package org.linkwave.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Giới hạn đăng nhập, lưu trong Redis:
 * - Theo tên đăng nhập: sai MAX_FAILURES lần trong vòng WINDOW thì tạm khóa LOCK_DURATION.
 * - Theo IP: mỗi IP tối đa MAX_IP_ATTEMPTS lần thử đăng nhập trong IP_WINDOW (chặn dò mật khẩu trên nhiều tài khoản).
 *
 * Trạng thái nằm trong Redis nên vẫn còn hiệu lực khi auth-service khởi động lại
 * và áp dụng đúng khi chạy nhiều bản auth-service.
 *
 * Nếu Redis không truy cập được thì bỏ qua việc giới hạn (fail-open) và ghi log,
 * để lỗi Redis không chặn toàn bộ đăng nhập.
 */
@Slf4j
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /** Đủ rộng cho cả một lớp đăng nhập qua cùng một NAT, nhưng vẫn chặn được dò mật khẩu hàng loạt */
    static final int MAX_IP_ATTEMPTS = 60;
    static final Duration IP_WINDOW = Duration.ofMinutes(1);

    private static final String FAIL_PREFIX = "auth:login:fail:";
    private static final String LOCK_PREFIX = "auth:login:lock:";
    private static final String IP_PREFIX = "auth:login:ip:";

    private final StringRedisTemplate redis;
    private final boolean trustForwardedFor;

    /**
     * trustForwardedFor = true khi auth-service nằm sau api-gateway: gateway thêm IP thật của client vào cuối X-Forwarded-For.
     * Chỉ bật khi auth-service không mở trực tiếp ra ngoài, nếu không client có thể tự đặt X-Forwarded-For.
     */
    public LoginAttemptService(StringRedisTemplate redis,
                               @Value("${auth.trust-forwarded-for:true}") boolean trustForwardedFor) {
        this.redis = redis;
        this.trustForwardedFor = trustForwardedFor;
    }

    /** Thời gian còn lại của lần khóa, Duration.ZERO nếu không bị khóa. */
    public Duration lockedFor(String username) {
        try {
            final Long ttl = redis.getExpire(LOCK_PREFIX + key(username));
            if (ttl == null || ttl <= 0) {
                return Duration.ZERO;
            }
            return Duration.ofSeconds(ttl);
        } catch (DataAccessException e) {
            log.error("-> lockedFor(): không truy cập được Redis, bỏ qua giới hạn đăng nhập: {}", e.getMessage());
            return Duration.ZERO;
        }
    }

    /** Ghi nhận một lần thử đăng nhập từ IP này. Trả true nếu IP đã vượt giới hạn trong cửa sổ hiện tại. */
    public boolean ipExceeded(String ip) {
        try {
            final Long attempts = redis.opsForValue().increment(IP_PREFIX + ip);
            if (attempts == null) {
                return false;
            }
            if (attempts == 1L) {
                redis.expire(IP_PREFIX + ip, IP_WINDOW);
            }
            return attempts > MAX_IP_ATTEMPTS;
        } catch (DataAccessException e) {
            log.error("-> ipExceeded(): không truy cập được Redis, bỏ qua giới hạn theo IP: {}", e.getMessage());
            return false;
        }
    }

    /** IP của client: lấy từ X-Forwarded-For (phần tử cuối do gateway thêm) nếu được tin cậy, không thì lấy địa chỉ kết nối. */
    public String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            final String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                final String[] parts = forwarded.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public void recordFailure(String username) {
        final String user = key(username);
        try {
            final Long failures = redis.opsForValue().increment(FAIL_PREFIX + user);
            if (failures == null) {
                return;
            }
            if (failures == 1L) {
                // Bắt đầu cửa sổ đếm: sau WINDOW thì bộ đếm tự hết hạn
                redis.expire(FAIL_PREFIX + user, WINDOW);
            }
            if (failures >= MAX_FAILURES) {
                redis.opsForValue().set(LOCK_PREFIX + user, "1", LOCK_DURATION);
                redis.delete(FAIL_PREFIX + user);
                log.warn("-> recordFailure(): tài khoản {} bị khóa tạm {} phút", user, LOCK_DURATION.toMinutes());
            }
        } catch (DataAccessException e) {
            log.error("-> recordFailure(): không truy cập được Redis, không ghi nhận lần sai: {}", e.getMessage());
        }
    }

    public void reset(String username) {
        final String user = key(username);
        try {
            redis.delete(FAIL_PREFIX + user);
            redis.delete(LOCK_PREFIX + user);
        } catch (DataAccessException e) {
            log.error("-> reset(): không truy cập được Redis: {}", e.getMessage());
        }
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}