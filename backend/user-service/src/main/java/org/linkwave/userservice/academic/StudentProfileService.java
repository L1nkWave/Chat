package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Hồ sơ sinh viên cho màn hình quản trị. CCCD chỉ giải mã khi được yêu cầu rõ ràng (đã kiểm tra quyền và ghi nhật ký ở controller). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProfileService {

    /** cccdMasked là null nếu sinh viên chưa có CCCD */
    public record StudentInfo(String studentCode, String classCode, String cccdMasked) {
    }

    private final JdbcTemplate jdbc;
    private final CccdCipher cipher;

    public Optional<StudentInfo> find(Long userId) {
        return jdbc.query("select student_code, class_code, cccd_enc from student_profiles where user_id = ?",
                        (rs, n) -> {
                            final String encrypted = rs.getString("cccd_enc");
                            final String masked = encrypted == null
                                    ? null
                                    : CccdCipher.mask(cipher.decrypt(encrypted));
                            return new StudentInfo(rs.getString("student_code"), rs.getString("class_code"), masked);
                        },
                        userId)
                .stream()
                .findFirst();
    }

    /** Giải mã đầy đủ CCCD. Trả về Optional.empty nếu không có hồ sơ hoặc chưa có CCCD. */
    public Optional<String> revealCccd(Long userId) {
        final List<String> encrypted = jdbc.query("select cccd_enc from student_profiles where user_id = ?",
                (rs, n) -> rs.getString(1), userId);
        return encrypted.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(cipher::decrypt);
    }
}