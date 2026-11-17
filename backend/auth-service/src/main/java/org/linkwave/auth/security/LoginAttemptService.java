package org.linkwave.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Đếm số lần đăng nhập sai theo tên đăng nhập, lưu trong Redis.
 * Sai MAX_FAILURES lần trong vòng WINDOW thì tạm khóa đăng nhập LOCK_DURATION.
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

    private static final String FAIL_PREFIX = "auth:login:fail:";
    private static final String LOCK_PREFIX = "auth:login:lock:";

    private final StringRedisTemplate redis;

    public LoginAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
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