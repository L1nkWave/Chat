package org.linkwave.chatservice.academic;

import org.linkwave.chatservice.api.academic.AcademicClient.AmbiguousLecturer;

import java.util.List;

/**
 * Kết quả một lần đồng bộ nhóm học vụ.
 *
 * @param created           số nhóm mới được tạo trong lần này
 * @param invitationsSent   số lời mời mới được tạo (cặp nhóm - người)
 * @param alreadyMember     số cặp nhóm - người đã là thành viên
 * @param skipped           số cặp nhóm - người đã có lời mời đang chờ nên không gửi lại
 * @param ambiguousTeachers giảng viên trùng họ tên nên không tự động thêm, cần xử lý thủ công
 */
public record SyncReport(
        int created,
        int invitationsSent,
        int alreadyMember,
        int skipped,
        List<AmbiguousLecturer> ambiguousTeachers
) {
}
