package org.linkwave.ws.lock;

import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.Token;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Mốc thu hồi phiên theo người dùng. auth-service ghi mốc này khi đổi mật khẩu, đăng xuất mọi thiết bị hoặc xóa tài khoản.
 * Token được cấp trước mốc là token cũ: không được mở WebSocket mới bằng token đó.
 */
@Slf4j
@Service
public class SessionRevocation {

    /** Khóa Redis: auth:revoked-after:{userId}, giá trị là epoch giây của mốc thu hồi */
    public static final String KEY_PREFIX = "auth:revoked-after:";

    /** Kênh Redis: auth-service gửi userId, mọi ws-server đóng phiên của user đó */
    public static final String CHANNEL = "auth:revoke";

    private final StringRedisTemplate redis;

    public SessionRevocation(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * true nếu token đã bị thu hồi. Khi Redis lỗi thì coi như đã thu hồi (fail-closed):
     * không mở kết nối mới khi chưa kiểm tra được.
     */
    public boolean isRevoked(Token token) {
        try {
            final String mark = redis.opsForValue().get(KEY_PREFIX + token.userId());
            if (mark == null) {
                return false;
            }
            return token.createdAt() == null || token.createdAt().getEpochSecond() < Long.parseLong(mark);
        } catch (DataAccessException | NumberFormatException e) {
            log.error("-> isRevoked(): không kiểm tra được mốc thu hồi, từ chối kết nối: {}", e.getMessage());
            return true;
        }
    }
}