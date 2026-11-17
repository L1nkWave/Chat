package org.linkwave.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Đọc trạng thái 2FA từ bảng totp_secrets do auth-service quản lý (cùng database lw-users).
 * Chỉ đọc, không ghi. Trả về null nếu không đọc được để giao diện hiện "Không xác định"
 * thay vì báo sai là chưa bật.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorStatusService {

    private final JdbcTemplate jdbc;

    public Boolean isEnabled(Long userId) {
        try {
            final Boolean enabled = jdbc.queryForObject(
                    "select count(*) > 0 from totp_secrets where user_id = ? and enabled = true",
                    Boolean.class,
                    userId);
            return Boolean.TRUE.equals(enabled);
        } catch (DataAccessException e) {
            log.warn("-> isEnabled(): không đọc được totp_secrets (auth-service đã tạo bảng chưa?): {}", e.getMessage());
            return null;
        }
    }
}