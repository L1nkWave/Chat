package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.PermissionEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.PermissionRepository;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AuditService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Sửa một vai trò đã có: bộ quyền và tên hiển thị. Chỉ ROOT.
 * Không sửa được ROLE_ROOT. Thay đổi quyền có hiệu lực ở lần kiểm tra quyền tiếp theo của mọi người đang giữ vai trò này.
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class AdminRoleEditController {

    private static final String ROLE_ROOT = "ROLE_ROOT";

    private final AccessPolicyService access;
    private final AuditService audit;
    private final RoleCatalogRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public record UpdatePermissionsRequest(@NotNull List<@NotBlank String> permissions) {
    }

    public record UpdateDisplayNameRequest(@NotBlank @Size(max = 128) String displayName) {
    }

    @PutMapping("/{name}/permissions")
    @ResponseStatus(NO_CONTENT)
    @Transactional
    public void updatePermissions(@PathVariable String name,
                                  @RequestBody @Valid UpdatePermissionsRequest body,
                                  HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);

        if (ROLE_ROOT.equals(name)) {
            throw new UnacceptableRequestDataException("Không sửa được quyền của ROLE_ROOT");
        }
        final RoleEntity role = findRole(name);

        final Set<String> codes = new HashSet<>(body.permissions());
        final List<PermissionEntity> found = permissionRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            throw new UnacceptableRequestDataException("Có quyền không tồn tại trong danh mục");
        }

        final String before = role.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("-");
        final String after = codes.stream().sorted().reduce((a, b) -> a + "," + b).orElse("-");

        role.setPermissions(new HashSet<>(found));
        roleRepository.save(role);
        audit.record(actor, "ROLE_PERMISSIONS_UPDATE", "ROLE", name, before + " -> " + after, ip(request));
    }

    /** Đổi tên tiếng Việt hiển thị. Không đổi mã kỹ thuật ROLE_... */
    @PutMapping("/{name}/display-name")
    @ResponseStatus(NO_CONTENT)
    @Transactional
    public void updateDisplayName(@PathVariable String name,
                                  @RequestBody @Valid UpdateDisplayNameRequest body,
                                  HttpServletRequest request) {
        final Long actor = currentAdminId();
        requireRoot(actor);

        final RoleEntity role = findRole(name);
        final String newName = body.displayName().trim();
        if (newName.isEmpty()) {
            throw new UnacceptableRequestDataException("Tên hiển thị không được để trống");
        }
        final String before = role.getDisplayName() == null ? "-" : role.getDisplayName();

        role.setDisplayName(newName);
        roleRepository.save(role);
        audit.record(actor, "ROLE_RENAME", "ROLE", name, before + " -> " + newName, ip(request));
    }

    private RoleEntity findRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + name));
    }

    private void requireRoot(Long actor) {
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được sửa vai trò");
        }
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