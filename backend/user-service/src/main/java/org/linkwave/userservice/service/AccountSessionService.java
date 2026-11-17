package org.linkwave.userservice.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.userservice.api.ws.WsSessionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Đồng bộ trạng thái khóa sang WebSocket: khóa thì đóng mọi kết nối đang mở,
 * mở khóa thì cho phép cấp vé kết nối lại.
 * Lỗi gọi ws-server không làm hỏng thao tác khóa; token hết hạn sau tối đa 5 phút vẫn chặn truy cập.
 */
@Slf4j
@Service
public class AccountSessionService {

    private final WsSessionClient wsClient;
    private final String internalKey;

    public AccountSessionService(WsSessionClient wsClient,
                                 @Value("${internal.service-key}") String internalKey) {
        this.wsClient = wsClient;
        this.internalKey = internalKey;
    }

    public void setLocked(Long userId, boolean locked) {
        try {
            if (locked) {
                wsClient.lock(internalKey, userId);
            } else {
                wsClient.unlock(internalKey, userId);
            }
        } catch (FeignException e) {
            log.warn("Không đồng bộ được WebSocket cho user {} (locked={}): {}", userId, locked, e.getMessage());
        }
    }
}