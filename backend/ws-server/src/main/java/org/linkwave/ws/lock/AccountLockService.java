package org.linkwave.ws.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.session.SessionManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Khóa tài khoản ở tầng WebSocket: đóng mọi kết nối đang mở và chặn cấp vé kết nối mới.
 * Trạng thái khóa nằm trong Redis nên vẫn còn hiệu lực sau khi ws-server khởi động lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockService {

    /** Mã 4401 mà chat-socket.ts coi là phiên hết hiệu lực, client sẽ chuyển về trang đăng nhập */
    public static final CloseStatus ACCOUNT_LOCKED = new CloseStatus(4401, "ACCOUNT_LOCKED");

    private static final String KEY_PREFIX = "ws:locked:";

    private final StringRedisTemplate redis;
    private final ChatRepository<Long, String> chatRepository;
    private final SessionManager sessionManager;

    public void lock(Long userId) {
        redis.opsForValue().set(KEY_PREFIX + userId, "1");

        int closed = 0;
        for (String sessionId : chatRepository.getUserSessions(userId)) {
            final WebSocketSession session = sessionManager.find(sessionId);
            if (session == null) {
                // Phiên thuộc instance khác: không đóng được tại đây
                continue;
            }
            try {
                session.close(ACCOUNT_LOCKED);
                closed++;
            } catch (IOException | RuntimeException e) {
                log.warn("-> lock(): không đóng được phiên {} của user {}: {}", sessionId, userId, e.getMessage());
            }
        }
        log.info("-> lock(): user {} bị khóa, đã đóng {} phiên WebSocket", userId, closed);
    }

    public void unlock(Long userId) {
        redis.delete(KEY_PREFIX + userId);
    }

    public boolean isLocked(Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + userId));
    }
}