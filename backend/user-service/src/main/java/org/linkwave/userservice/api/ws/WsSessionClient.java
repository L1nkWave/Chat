package org.linkwave.userservice.api.ws;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/** Gọi API nội bộ của ws-server để đóng hoặc mở kết nối WebSocket của một tài khoản. */
@FeignClient(value = "ws-server", path = "/api/v1/internal/accounts")
public interface WsSessionClient {

    String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @PostMapping("/{userId}/lock")
    void lock(@RequestHeader(INTERNAL_KEY_HEADER) String internalKey, @PathVariable Long userId);

    @PostMapping("/{userId}/unlock")
    void unlock(@RequestHeader(INTERNAL_KEY_HEADER) String internalKey, @PathVariable Long userId);
}