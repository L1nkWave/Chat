package org.linkwave.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
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

import java.time.ZonedDateTime;
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

    private final AccessPolicyService access;
    private final UserRepository userRepository;
    private final ScopedRoleRepository scopedRoleRepository;

    /** Một dòng trong danh sách */
    public record UserRow(Long id, String username, String name, boolean locked,
                          List<String> roles, ZonedDateTime createdAt, ZonedDateTime lastSeen) {
    }

    public record UserPage(List<UserRow> items, long total, int page, int size) {
    }

    public record ScopedGrantView(Long grantId, String role, String scopeType, Long scopeId,
                                  ZonedDateTime grantedAt, Long grantedBy) {
    }

    /** Hồ sơ đầy đủ. schoolPermissions là quyền thực tế ở phạm vi toàn trường. */
    public record UserDetailView(Long id, String username, String name, String bio, boolean locked,
                                 ZonedDateTime createdAt, ZonedDateTime lastSeen,
                                 List<String> roles, List<ScopedGrantView> scopedRoles,
                                 List<String> schoolPermissions) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UserPage search(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String role,
                           @RequestParam(defaultValue = "ALL") String status,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        requireUserView();

        final String statusValue = STATUSES.contains(status) ? status : "ALL";
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), 100);
        final String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";

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
                permissions);
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