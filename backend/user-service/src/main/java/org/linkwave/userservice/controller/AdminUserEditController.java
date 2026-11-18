package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.StudentProfileService;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AuditService;
import org.linkwave.userservice.service.TemporaryPasswords;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Sửa thông tin người dùng từ màn hình quản trị: họ tên, tiểu sử, đặt lại mật khẩu tạm, đổi vai trò toàn trường.
 * Tên đăng nhập không đổi ở đây. Mỗi thao tác kiểm tra quyền, cấp bậc và ghi nhật ký.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserEditController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;
    private static final Set<String> EDITABLE_ROLES = Set.of("ROLE_STUDENT", "ROLE_LECTURER", "ROLE_ADMIN");
    private static final String ROLE_ROOT = "ROLE_ROOT";
    private final AccessPolicyService access;
    private final AuditService audit;
    private final UserRepository userRepository;
    private final RoleCatalogRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentProfileService students;

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 64) String name,
            @Size(max = 255) String bio
    ) {
    }

    public record SetGlobalRoleRequest(
            @NotBlank @Pattern(regexp = "ROLE_(STUDENT|LECTURER|ADMIN)", message = "vai trò không hợp lệ")
            String role
    ) {
    }

    /** Mật khẩu tạm chỉ trả về một lần trong phản hồi này, không được ghi vào nhật ký */
    public record ResetPasswordResponse(String temporaryPassword) {
    }

    /** CCCD đầy đủ, chỉ trả về khi admin bấm "Hiện đầy đủ" */
    public record CccdView(String cccd) {
    }

    /** Sửa họ tên và tiểu sử. Cần user.view và cấp cao hơn người được sửa (hoặc chính mình). */
    @PatchMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    @Transactional
    public void updateProfile(@PathVariable Long id,
                              @RequestBody @Valid UpdateProfileRequest body,
                              HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "user.view", SCHOOL, null);
        access.requireCanManage(actor, id);

        final String name = body.name().trim();
        if (name.isEmpty()) {
            throw new UnacceptableRequestDataException("Họ tên không được để trống");
        }
        final String bio = body.bio() == null || body.bio().isBlank() ? null : body.bio().trim();

        final UserEntity user = findUser(id);
        user.setName(name);
        user.setBio(bio);
        userRepository.save(user);
        audit.record(actor, "USER_EDIT", "USER", String.valueOf(id), "name,bio", ip(request));
    }

    /**
     * Đặt mật khẩu tạm ngẫu nhiên. Người dùng phải đổi mật khẩu ở lần đăng nhập tiếp theo,
     * và mọi phiên đang có (refresh token cũ) bị thu hồi.
     */
    @PostMapping("/{id}/reset-password")
    @Transactional
    public ResetPasswordResponse resetPassword(@PathVariable Long id, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "user.lock", SCHOOL, null);
        if (actor.equals(id)) {
            throw new UnacceptableRequestDataException("Không thể tự đặt lại mật khẩu của chính mình. Hãy dùng chức năng đổi mật khẩu.");
        }
        access.requireCanManage(actor, id);

        final UserEntity user = findUser(id);
        final String temporary = TemporaryPasswords.generate(user.getUsername());
        user.setPassword(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setTokensValidAfter(revocationMark());
        userRepository.save(user);
        audit.record(actor, "PASSWORD_RESET", "USER", String.valueOf(id), null, ip(request));
        return new ResetPasswordResponse(temporary);
    }

    /**
     * Hiện đầy đủ CCCD của sinh viên. Mỗi lần xem đều ghi nhật ký CCCD_REVEAL (không ghi số CCCD vào nhật ký).
     */
    @PostMapping("/{id}/cccd/reveal")
    public CccdView revealCccd(@PathVariable Long id, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "user.view", SCHOOL, null);
        access.requireCanManage(actor, id);

        final String cccd = students.revealCccd(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng này chưa có CCCD"));
        audit.record(actor, "CCCD_REVEAL", "USER", String.valueOf(id), null, ip(request));
        return new CccdView(cccd);
    }

    /**
     * Chỉ ROOT. Đặt vai trò toàn trường của người dùng là STUDENT, LECTURER hoặc ADMIN (thay thế vai trò cũ trong nhóm này).
     * Không đổi được ROOT, không tự đổi chính mình. Người dùng bị đăng xuất để nhận quyền mới khi đăng nhập lại.
     */
    @PutMapping("/{id}/global-role")
    @ResponseStatus(NO_CONTENT)
    @Transactional
    public void setGlobalRole(@PathVariable Long id,
                              @RequestBody @Valid SetGlobalRoleRequest body,
                              HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);
        if (actor.equals(id)) {
            throw new UnacceptableRequestDataException("Không thể tự đổi vai trò của chính mình");
        }

        final UserEntity user = findUser(id);
        final List<String> current = user.getRoles().stream().map(RoleEntity::getName).toList();
        if (current.contains(ROLE_ROOT)) {
            throw new UnacceptableRequestDataException("Không thể đổi vai trò của quản trị cao nhất");
        }
        final RoleEntity target = roleRepository.findByName(body.role())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + body.role()));

        final String before = current.stream()
                .filter(EDITABLE_ROLES::contains)
                .sorted()
                .collect(Collectors.joining(","));

        final List<RoleEntity> updated = user.getRoles().stream()
                .filter(role -> !EDITABLE_ROLES.contains(role.getName()))
                .collect(Collectors.toCollection(ArrayList::new));
        updated.add(target);
        user.setRoles(updated);
        user.setTokensValidAfter(revocationMark());
        userRepository.save(user);

        audit.record(actor, "ROLE_SET", "USER", String.valueOf(id),
                (before.isEmpty() ? "-" : before) + " -> " + body.role(), ip(request));
    }

    private void requireRoot(Long actor) {
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được thực hiện thao tác này");
        }
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    /** Cùng quy ước với auth-service: refresh token được cấp trước mốc này sẽ bị từ chối */
    private static Instant revocationMark() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
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