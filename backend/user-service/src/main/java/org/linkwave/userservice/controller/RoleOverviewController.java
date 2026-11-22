package org.linkwave.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopedRoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/** Chỉ ROOT. Tổng quan vai trò: mỗi vai trò có bao nhiêu người, và danh sách người đó. */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleOverviewController {

    private final AccessPolicyService access;
    private final RoleCatalogRepository roleRepository;
    private final ScopedRoleRepository scopedRoleRepository;

    /**
     * name: mã kỹ thuật (ROLE_...). displayName: tên tiếng Việt, có thể null.
     * globalMembers: người được gán vai trò toàn trường; scopedMembers: người có lần cấp theo phạm vi.
     */
    public record RoleOverview(Integer id, String name, String displayName, List<String> permissions,
                               int globalMembers, int scopedMembers) {
    }

    /** source: GLOBAL hoặc SCOPED; scopeType/scopeId chỉ có khi SCOPED */
    public record MemberView(Long userId, String username, String name, String source,
                             String scopeType, Long scopeId, Long grantId) {
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public List<RoleOverview> overview() {
        requireRoot();

        final Map<String, Set<Long>> scopedUsers = new HashMap<>();
        for (ScopedRoleEntity grant : scopedRoleRepository.findAllActive()) {
            scopedUsers.computeIfAbsent(grant.getRole().getName(), key -> new HashSet<>())
                    .add(grant.getUser().getId());
        }

        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleEntity::getName))
                .map(role -> new RoleOverview(
                        role.getId(),
                        role.getName(),
                        role.getDisplayName(),
                        role.getPermissions().stream().map(permission -> permission.getCode()).sorted().toList(),
                        (int) role.getUsers().stream().filter(user -> !user.isDeleted()).count(),
                        scopedUsers.getOrDefault(role.getName(), Set.of()).size()))
                .toList();
    }

    /** Một trang thành viên: total là tổng số người có vai trò, items là các người thuộc trang này */
    public record MemberPage(long total, List<MemberView> items) {
    }

    @GetMapping("/{name}/members")
    @Transactional(readOnly = true)
    public MemberPage members(@PathVariable String name,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        requireRoot();

        final RoleEntity role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò " + name));

        final List<MemberView> all = new ArrayList<>();
        role.getUsers().stream()
                .filter(user -> !user.isDeleted())
                .sorted(Comparator.comparing(UserEntity::getName))
                .forEach(user -> all.add(new MemberView(
                        user.getId(), user.getUsername(), user.getName(), "GLOBAL", null, null, null)));

        for (ScopedRoleEntity grant : scopedRoleRepository.findAllActive()) {
            if (grant.getRole().getName().equals(name)) {
                all.add(new MemberView(
                        grant.getUser().getId(),
                        grant.getUser().getUsername(),
                        grant.getUser().getName(),
                        "SCOPED",
                        grant.getScopeType().name(),
                        grant.getScopeId(),
                        grant.getId()));
            }
        }

        final int pageSize = Math.clamp(size, 1, 100);
        final int from = Math.min(Math.max(page, 0) * pageSize, all.size());
        final int to = Math.min(from + pageSize, all.size());
        return new MemberPage(all.size(), all.subList(from, to));
    }

    private void requireRoot() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new ResponseStatusException(FORBIDDEN, "Cần bật và xác thực hai lớp trước khi dùng trang quản trị");
        }
        final Long actor = ((DefaultUserDetails) authentication.getPrincipal()).id();
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được xem tổng quan vai trò");
        }
    }
}