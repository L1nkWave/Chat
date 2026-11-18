package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.UserProfileService;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Tạo tài khoản sinh viên, giảng viên hoặc quản trị viên từ màn hình quản trị.
 * Mật khẩu mặc định Dhsg@123, người dùng phải đổi ở lần đăng nhập đầu.
 * Sinh viên phải thuộc lớp đã có trong danh mục. Chỉ ROOT được tạo ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserCreateController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;
    private static final String DEFAULT_PASSWORD = "Dhsg@123";
    private static final String ROLE_STUDENT = "ROLE_STUDENT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final java.util.regex.Pattern USERNAME =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._-]{3,32}$");

    private final AccessPolicyService access;
    private final AuditService audit;
    private final UserRepository userRepository;
    private final RoleCatalogRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final UserProfileService profiles;

    public record CreateUserRequest(
            @NotBlank @Size(max = 32) String username,
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Pattern(regexp = "ROLE_(STUDENT|LECTURER|ADMIN)", message = "vai trò không hợp lệ")
            String role,
            @Size(max = 32) String classCode,
            String gender,
            LocalDate birthDate,
            @NotBlank String status,
            String phone,
            String birthplace,
            String nationality,
            String address,
            String cccd,
            String ethnicity,
            String religion) {
    }

    public record CreatedUser(Long id, String username) {
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Transactional
    public CreatedUser create(@RequestBody @Valid CreateUserRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "user.view", SCHOOL, null);
        if (ROLE_ADMIN.equals(body.role())) {
            requireRoot(actor);
        }

        final String username = body.username().trim();
        if (!USERNAME.matcher(username).matches()) {
            throw new UnacceptableRequestDataException(
                    "Mã tài khoản chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang, từ 3 đến 32 ký tự");
        }
        final String name = body.name().trim();
        if (name.isEmpty()) {
            throw new UnacceptableRequestDataException("Họ tên không được để trống");
        }
        final Integer taken = jdbc.queryForObject("select count(*) from users where username = ?", Integer.class, username);
        if (taken != null && taken > 0) {
            throw new UnacceptableRequestDataException("Mã tài khoản " + username + " đã tồn tại");
        }

        final String classCode = blankToNull(body.classCode());
        if (ROLE_STUDENT.equals(body.role())) {
            if (classCode == null) {
                throw new UnacceptableRequestDataException("Sinh viên phải thuộc một lớp");
            }
            final Integer classCount = jdbc.queryForObject(
                    "select count(*) from school_classes where code = ?", Integer.class, classCode);
            if (classCount == null || classCount == 0) {
                throw new UnacceptableRequestDataException("Lớp " + classCode + " chưa có trong danh mục");
            }
        }

        final RoleEntity role = roleRepository.findByName(body.role())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + body.role()));
        final List<RoleEntity> roles = new ArrayList<>();
        roles.add(role);
        roleRepository.findByName("ROLE_USER").ifPresent(roles::add);

        final UserEntity user = userRepository.save(UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .name(name)
                .roles(roles)
                .mustChangePassword(true)
                .build());

        if (ROLE_STUDENT.equals(body.role())) {
            jdbc.update(
                    "insert into student_profiles (user_id, student_code, class_code, updated_at) values (?, ?, ?, now())",
                    user.getId(), username, classCode);
        }

        profiles.save(user.getId(), body.role(), new UserProfileService.ProfileInput(
                blankToNull(body.gender()),
                body.birthDate(),
                blankToNull(body.status()),
                body.phone(),
                body.birthplace(),
                body.nationality(),
                body.address(),
                blankToNull(body.cccd()),
                blankToNull(body.ethnicity()),
                blankToNull(body.religion())));

        audit.record(actor, "USER_CREATE", "USER", String.valueOf(user.getId()), body.role(), ip(request));
        return new CreatedUser(user.getId(), username);
    }

    private void requireRoot(Long actor) {
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được tạo quản trị viên");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Lấy ID người gọi. ADMIN/ROOT phải có cờ MFA_VERIFIED (đã bật và xác thực 2FA). */
    private Long currentAdminId() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new ResponseStatusException(FORBIDDEN, "Cần bật và xác thực hai lớp trước khi dùng trang quản trị");
        }
        return ((DefaultUserDetails) authentication.getPrincipal()).id();
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}