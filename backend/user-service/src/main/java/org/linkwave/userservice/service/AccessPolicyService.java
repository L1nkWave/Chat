package org.linkwave.userservice.service;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.ScopedRoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.repository.SchoolClassRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Kiểm tra quyền theo phạm vi.
 * - Vai trò gán toàn cục (user_roles) có hiệu lực trên toàn trường.
 * - Vai trò gán theo phạm vi (scoped_roles) chỉ có hiệu lực khi phạm vi yêu cầu được bao phủ.
 * Phạm vi SCHOOL bao phủ mọi phạm vi; CLASS chỉ bao phủ đúng lớp đó;
 * FACULTY bao phủ đúng khoa đó và mọi lớp thuộc khoa.
 */
@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final UserRepository userRepository;
    private final ScopedRoleRepository scopedRoleRepository;
    private final SchoolClassRepository classRepository;

    @Transactional(readOnly = true)
    public Set<String> permissionsFor(Long userId, ScopeType scopeType, Long scopeId) {
        final UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final Set<String> codes = new HashSet<>();

        for (RoleEntity role : user.getRoles()) {
            role.getPermissions().forEach(permission -> codes.add(permission.getCode()));
        }

        for (ScopedRoleEntity grant : scopedRoleRepository.findActiveByUserId(userId)) {
            if (covers(grant, scopeType, scopeId)) {
                grant.getRole().getPermissions().forEach(permission -> codes.add(permission.getCode()));
            }
        }

        return codes;
    }

    @Transactional(readOnly = true)
    public boolean can(Long userId, String permission, ScopeType scopeType, Long scopeId) {
        return permissionsFor(userId, scopeType, scopeId).contains(permission);
    }

    @Transactional(readOnly = true)
    public void require(Long userId, String permission, ScopeType scopeType, Long scopeId) {
        if (!can(userId, permission, scopeType, scopeId)) {
            throw new ResponseStatusException(FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        }
    }

    /** Cấp cao nhất trong các vai trò toàn cục của tài khoản. */
    @Transactional(readOnly = true)
    public int levelOf(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .mapToInt(role -> RoleEntity.Roles.levelOf(role.getName()))
                        .max()
                        .orElse(0))
                .orElse(0);
    }

    /**
     * Người thao tác phải có cấp cao hơn đối tượng.
     * Ngoại lệ: ROOT được tác động lên ROOT khác. Tự thao tác lên chính mình luôn được phép.
     */
    @Transactional(readOnly = true)
    public void requireCanManage(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) {
            return;
        }
        final int actorLevel = levelOf(actorId);
        final int targetLevel = levelOf(targetId);
        final int rootLevel = RoleEntity.Roles.ROOT.getLevel();
        final boolean rootOverRoot = actorLevel == rootLevel && targetLevel == rootLevel;

        if (actorLevel <= targetLevel && !rootOverRoot) {
            throw new ResponseStatusException(FORBIDDEN, "Không thể tác động lên tài khoản có cấp bằng hoặc cao hơn");
        }
    }

    private boolean covers(ScopedRoleEntity grant, ScopeType scopeType, Long scopeId) {
        if (grant.getScopeType() == ScopeType.SCHOOL) {
            return true;
        }
        if (grant.getScopeType() == scopeType && Objects.equals(grant.getScopeId(), scopeId)) {
            return true;
        }
        // FACULTY bao phủ mọi lớp thuộc khoa đó
        return grant.getScopeType() == ScopeType.FACULTY
                && scopeType == ScopeType.CLASS
                && scopeId != null
                && classRepository.findById(scopeId)
                .map(schoolClass -> Objects.equals(schoolClass.getFaculty().getId(), grant.getScopeId()))
                .orElse(false);
    }

}