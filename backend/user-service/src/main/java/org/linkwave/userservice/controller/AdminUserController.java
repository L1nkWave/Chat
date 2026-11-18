package org.linkwave.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.StudentProfileService;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.AuditLogRepository;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.TwoFactorStatusService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/** Tìm kiếm và xem hồ sơ người dùng cho màn hình quản trị. Yêu cầu quyền user.view. */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;
    private static final Set<String> STATUSES = Set.of("ALL", "LOCKED", "ACTIVE");
    private static final int RECENT_AUDIT_SIZE = 10;
    private static final ZonedDateTime EARLIEST = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final ZonedDateTime LATEST = ZonedDateTime.of(9999, 12, 31, 0, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));

    private final AccessPolicyService access;
    private final UserRepository userRepository;
    private final ScopedRoleRepository scopedRoleRepository;
    private final TwoFactorStatusService twoFactorStatus;
    private final AuditLogRepository auditRepository;
    private final StudentProfileService students;
    private final JdbcTemplate jdbc;

    /** Một dòng trong danh sách */
    public record UserRow(Long id, String username, String name, boolean locked,
                          List<String> roles, ZonedDateTime createdAt, ZonedDateTime lastSeen) {
    }

    public record UserPage(List<UserRow> items, long total, int page, int size) {
    }

    public record ScopedGrantView(Long grantId, String role, String scopeType, Long scopeId,
                                  ZonedDateTime grantedAt, Long grantedBy) {
    }

    /**
     * Hồ sơ đầy đủ. schoolPermissions là quyền thực tế ở phạm vi toàn trường.
     * twoFactorEnabled là null nếu không đọc được trạng thái 2FA.
     * recentAudit là các hoạt động gần nhất có đối tượng là người dùng này.
     */
    public record UserDetailView(Long id, String username, String name, String bio, boolean locked,
                                 ZonedDateTime createdAt, ZonedDateTime lastSeen,
                                 List<String> roles, List<ScopedGrantView> scopedRoles,
                                 List<String> schoolPermissions,
                                 Boolean twoFactorEnabled,
                                 List<AdminAuditController.AuditRow> recentAudit,
                                 String studentCode, String classCode, String cccdMasked) {
    }

    /**
     * Tìm người dùng. faculty (mã khoa), classCode (mã lớp), subject (mã môn) lọc theo học vụ:
     * sinh viên theo khoa, lớp, hoặc lớp có môn; giảng viên dạy môn đó (khi không lọc theo khoa hoặc lớp).
     */
    @GetMapping
    @Transactional(readOnly = true)
    public UserPage search(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String role,
                           @RequestParam(defaultValue = "ALL") String status,
                           @RequestParam(defaultValue = "") String faculty,
                           @RequestParam(defaultValue = "") String classCode,
                           @RequestParam(defaultValue = "") String subject,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        requireUserView();

        final String statusValue = STATUSES.contains(status) ? status : "ALL";
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), 100);
        final String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        final String facultyCode = faculty.trim();
        final String classValue = classCode.trim();
        final String subjectCode = subject.trim();

        if (facultyCode.isEmpty() && classValue.isEmpty() && subjectCode.isEmpty()) {
            final Page<UserEntity> result = userRepository.searchForAdmin(
                    pattern,
                    role.trim(),
                    statusValue,
                    PageRequest.of(safePage, safeSize, Sort.by("name")));
            final List<UserRow> items = result.getContent().stream()
                    .map(AdminUserController::toRow)
                    .toList();
            return new UserPage(items, result.getTotalElements(), safePage, safeSize);
        }

        // Có lọc học vụ: lấy danh sách khớp học vụ rồi lọc và phân trang trong bộ nhớ (quy mô một trường)
        final Set<Long> academicIds = academicUserIds(facultyCode, classValue, subjectCode);
        final List<UserEntity> matched = userRepository.searchForAdmin(
                        pattern, role.trim(), statusValue, PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name")))
                .getContent().stream()
                .filter(user -> academicIds.contains(user.getId()))
                .toList();
        final int from = Math.min(safePage * safeSize, matched.size());
        final int to = Math.min(from + safeSize, matched.size());
        final List<UserRow> items = matched.subList(from, to).stream()
                .map(AdminUserController::toRow)
                .toList();
        return new UserPage(items, matched.size(), safePage, safeSize);
    }

    /** Id người dùng khớp bộ lọc học vụ. Tham số rỗng nghĩa là không lọc theo tiêu chí đó. */
    private Set<Long> academicUserIds(String facultyCode, String classCode, String subjectCode) {
        final Set<Long> ids = new HashSet<>();
        ids.addAll(jdbc.queryForList("""
                        select p.user_id
                        from student_profiles p
                        join school_classes c on c.code = p.class_code
                        join faculties f on f.id = c.faculty_id
                        where (? = '' or f.code = ?)
                          and (? = '' or p.class_code = ?)
                          and (? = '' or p.class_code in (select class_code from course_sessions where subject_code = ?))
                        """, Long.class,
                facultyCode, facultyCode, classCode, classCode, subjectCode, subjectCode));

        // Giảng viên không gắn khoa trong hệ thống, nên chỉ lọc theo môn khi không lọc theo khoa hoặc lớp
        if (!subjectCode.isEmpty() && facultyCode.isEmpty() && classCode.isEmpty()) {
            ids.addAll(jdbc.queryForList("""
                            select u.id from users u
                            where u.username in (select lecturer_code from course_sessions where subject_code = ?)
                            """, Long.class, subjectCode));
        }
        return ids;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserDetailView detail(@PathVariable Long id) {
        requireUserView();

        final UserEntity user = userRepository.findById(id)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        final List<ScopedGrantView> grants = scopedRoleRepository.findActiveByUserId(id).stream()
                .map(grant -> new ScopedGrantView(
                        grant.getId(),
                        grant.getRole().getName(),
                        grant.getScopeType().name(),
                        grant.getScopeId(),
                        grant.getGrantedAt(),
                        grant.getGrantedBy()))
                .toList();

        final List<String> permissions = access.permissionsFor(id, SCHOOL, null).stream().sorted().toList();

        final List<AdminAuditController.AuditRow> recentAudit = auditRepository.search(
                        "",
                        0L,
                        String.valueOf(id),
                        EARLIEST,
                        LATEST,
                        PageRequest.of(0, RECENT_AUDIT_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(entry -> new AdminAuditController.AuditRow(
                        entry.getId(),
                        entry.getActorId(),
                        entry.getAction(),
                        entry.getTargetType(),
                        entry.getTargetId(),
                        entry.getDetail(),
                        entry.getIp(),
                        entry.getCreatedAt()))
                .toList();

        final StudentProfileService.StudentInfo student = students.find(id).orElse(null);

        return new UserDetailView(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.isBlocked(),
                user.getCreatedAt(),
                user.getLastSeen(),
                user.getRoles().stream().map(RoleEntity::getName).sorted().toList(),
                grants,
                permissions,
                twoFactorStatus.isEnabled(id),
                recentAudit,
                student == null ? null : student.studentCode(),
                student == null ? null : student.classCode(),
                student == null ? null : student.cccdMasked());
    }

    private static UserRow toRow(UserEntity user) {
        return new UserRow(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.isBlocked(),
                user.getRoles().stream().map(RoleEntity::getName).sorted().toList(),
                user.getCreatedAt(),
                user.getLastSeen());
    }

    private void requireUserView() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new ResponseStatusException(FORBIDDEN, "Cần bật và xác thực hai lớp trước khi dùng trang quản trị");
        }
        final Long actor = ((DefaultUserDetails) authentication.getPrincipal()).id();
        access.require(actor, "user.view", SCHOOL, null);
    }
}