package org.linkwave.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Báo cho ws-server biết các phiên của người dùng đã bị thu hồi (đổi mật khẩu, đăng xuất mọi thiết bị, xóa tài khoản).
 * Ghi mốc vào Redis để chặn kết nối WebSocket mới bằng token cũ, và phát tin trên kênh để đóng phiên đang mở.
 * Lỗi Redis không làm hỏng thao tác chính; token HTTP vẫn bị thu hồi qua cơ sở dữ liệu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRevocationPublisher {

    /** Phải khớp với ws-server (SessionRevocation). */
    static final String KEY_PREFIX = "auth:revoked-after:";
    static final String CHANNEL = "auth:revoke";

    /** Access token sống vài phút, nên một ngày là đủ để chặn token cũ. */
    static final Duration MARK_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redis;

    public void revoke(Long userId, Instant mark) {
        try {
            redis.opsForValue().set(KEY_PREFIX + userId, String.valueOf(mark.getEpochSecond()), MARK_TTL);
            redis.convertAndSend(CHANNEL, String.valueOf(userId));
        } catch (DataAccessException e) {
            log.error("-> revoke(): không báo được ws-server cho user {}: {}", userId, e.getMessage());
        }
    }
}