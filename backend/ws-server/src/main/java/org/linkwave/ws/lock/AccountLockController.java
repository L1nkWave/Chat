package org.linkwave.ws.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * API nội bộ, chỉ user-service gọi khi khóa hoặc mở khóa tài khoản.
 * Không đi qua gateway; xác thực bằng khóa dùng chung INTERNAL_SERVICE_KEY.
 */
@RestController
@RequestMapping("/api/v1/internal/accounts")
@RequiredArgsConstructor
public class AccountLockController {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final AccountLockService lockService;

    @Value("${internal.service-key}")
    private String internalKey;

    @PostMapping("/{userId}/lock")
    @ResponseStatus(NO_CONTENT)
    public void lock(@RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String provided,
                     @PathVariable Long userId) {
        requireInternal(provided);
        lockService.lock(userId);
    }

    @PostMapping("/{userId}/unlock")
    @ResponseStatus(NO_CONTENT)
    public void unlock(@RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String provided,
                       @PathVariable Long userId) {
        requireInternal(provided);
        lockService.unlock(userId);
    }

    private void requireInternal(String provided) {
        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                internalKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(FORBIDDEN, "Không có quyền gọi API nội bộ");
        }
    }
}