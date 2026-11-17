package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.AuditLogEntity;
import org.linkwave.userservice.entity.PermissionEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.ScopedRoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.PermissionRepository;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AccountSessionService;
import org.linkwave.userservice.service.AuditService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/** API quản trị. Mỗi thao tác kiểm tra quyền, cấp bậc, và ghi nhật ký. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;

    private final AccessPolicyService access;
    private final AuditService audit;
    private final UserRepository userRepository;
    private final RoleCatalogRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ScopedRoleRepository scopedRoleRepository;
    private final AccountSessionService accountSessions;

    public record CreateRoleRequest(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{2,31}", message = "mã vai trò chỉ gồm chữ hoa, số, gạch dưới")
            String code,
            @NotNull List<@NotBlank String> permissions
    ) {
    }

    public record GrantRoleRequest(
            @NotBlank String role,
            @NotNull ScopeType scopeType,
            Long scopeId
    ) {
    }

    @GetMapping("/audit")
    public List<AuditLogEntity> audit(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        access.require(currentAdminId(), "audit.view", SCHOOL, null);
        return audit.list(page, size);
    }

    @PostMapping("/users/{id}/lock")
    @ResponseStatus(NO_CONTENT)
    public void lock(@PathVariable Long id, HttpServletRequest request) {
        setBlocked(id, true, "USER_LOCK", request);
    }

    @PostMapping("/users/{id}/unlock")
    @ResponseStatus(NO_CONTENT)
    public void unlock(@PathVariable Long id, HttpServletRequest request) {
        setBlocked(id, false, "USER_UNLOCK", request);
    }

    /** Chỉ ROOT tạo được vai trò nâng cao: đặt mã, gắn bộ quyền có sẵn. */
    @PostMapping("/roles")
    @ResponseStatus(CREATED)
    public void createRole(@RequestBody @Valid CreateRoleRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);

        final String name = "ROLE_" + body.code();
        if (roleRepository.existsByName(name)) {
            throw new UnacceptableRequestDataException("Vai trò " + name + " đã tồn tại");
        }
        final Set<String> codes = new HashSet<>(body.permissions());
        final List<PermissionEntity> found = permissionRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            throw new UnacceptableRequestDataException("Có quyền không tồn tại trong danh mục");
        }

        roleRepository.save(RoleEntity.builder()
                .name(name)
                .permissions(new HashSet<>(found))
                .build());
        audit.record(actor, "ROLE_CREATE", "ROLE", name, String.join(",", codes), ip(request));
    }

    /** Chỉ ROOT. Cấp vai trò theo phạm vi: SCHOOL (không mã), FACULTY hoặc CLASS (có mã). */
    @PostMapping("/users/{id}/roles")
    @ResponseStatus(CREATED)
    public void grantRole(@PathVariable Long id, @RequestBody @Valid GrantRoleRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);

        if ((body.scopeType() == SCHOOL) != (body.scopeId() == null)) {
            throw new UnacceptableRequestDataException("Phạm vi SCHOOL không có mã; FACULTY/CLASS phải có mã");
        }
        final UserEntity user = findUser(id);
        final RoleEntity role = roleRepository.findByName(body.role())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + body.role()));

        scopedRoleRepository.save(ScopedRoleEntity.builder()
                .user(user)
                .role(role)
                .scopeType(body.scopeType())
                .scopeId(body.scopeId())
                .grantedBy(actor)
                .build());
        audit.record(actor, "ROLE_GRANT", "USER", String.valueOf(id),
                body.role() + " " + body.scopeType() + (body.scopeId() == null ? "" : ":" + body.scopeId()),
                ip(request));
    }

    /** Chỉ ROOT. Thu hồi một lần cấp theo phạm vi (đánh dấu thời điểm, không xóa dòng). */
    @DeleteMapping("/roles/grants/{grantId}")
    @ResponseStatus(NO_CONTENT)
    public void revokeRole(@PathVariable Long grantId, HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);

        final ScopedRoleEntity grant = scopedRoleRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần cấp quyền"));
        // Chỉ thu hồi và ghi nhật ký ở lần đầu; gọi lại không tạo thêm dòng nhật ký
        if (grant.getRevokedAt() == null) {
            grant.setRevokedAt(ZonedDateTime.now());
            scopedRoleRepository.save(grant);
            audit.record(actor, "ROLE_REVOKE", "GRANT", String.valueOf(grantId), grant.getRole().getName(), ip(request));
        }
    }

    private void setBlocked(Long targetId, boolean blocked, String action, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "user.lock", SCHOOL, null);
        if (actor.equals(targetId)) {
            throw new UnacceptableRequestDataException("Không thể tự khóa hoặc mở khóa chính mình");
        }
        access.requireCanManage(actor, targetId);

        final UserEntity user = findUser(targetId);
        user.setBlocked(blocked);
        userRepository.save(user);
        // Đóng ngay các kết nối WebSocket đang mở của tài khoản (hoặc mở lại khi mở khóa)
        accountSessions.setLocked(targetId, blocked);
        audit.record(actor, action, "USER", String.valueOf(targetId), null, ip(request));
    }

    private void requireRoot(Long actor) {
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được thực hiện thao tác này");
        }
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
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