package org.linkwave.userservice.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.userservice.entity.PermissionEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.ScopedRoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.ScopedRoleRepository;
import org.linkwave.userservice.repository.SchoolClassRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScopedRoleRepository scopedRoleRepository;

    @Mock
    private SchoolClassRepository classRepository;

    @Test
    void globalAdminHasSchoolWideUserView() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        final UserEntity admin = user(1L, role("ROLE_ADMIN", "user.view"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(scopedRoleRepository.findActiveByUserId(1L)).thenReturn(List.of());

        assertTrue(service.can(1L, "user.view", ScopeType.SCHOOL, null));
        assertFalse(service.can(1L, "group.manage", ScopeType.SCHOOL, null));
    }

    @Test
    void studentWithoutPermissionsIsDenied() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        final UserEntity student = user(2L, role("ROLE_STUDENT"));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(scopedRoleRepository.findActiveByUserId(2L)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class,
                () -> service.require(2L, "user.view", ScopeType.SCHOOL, null));
    }

    @Test
    void facultyScopedLecturerIsLimitedToThatFaculty() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        final RoleEntity lecturerRole = role("ROLE_LECTURER", "group.manage");
        final UserEntity dean = user(3L, role("ROLE_STUDENT"));
        when(userRepository.findById(3L)).thenReturn(Optional.of(dean));
        when(scopedRoleRepository.findActiveByUserId(3L)).thenReturn(List.of(
                grant(dean, lecturerRole, ScopeType.FACULTY, 5L)));

        assertTrue(service.can(3L, "group.manage", ScopeType.FACULTY, 5L));
        assertFalse(service.can(3L, "group.manage", ScopeType.FACULTY, 6L));
        assertFalse(service.can(3L, "group.manage", ScopeType.SCHOOL, null));
    }

    @Test
    void lecturerCannotManageAdmin() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, role("ROLE_LECTURER"))));
        when(userRepository.findById(11L)).thenReturn(Optional.of(user(11L, role("ROLE_ADMIN"))));

        assertThrows(ResponseStatusException.class, () -> service.requireCanManage(10L, 11L));
    }

    @Test
    void adminCanManageStudentButNotRoot() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user(20L, role("ROLE_ADMIN"))));
        when(userRepository.findById(21L)).thenReturn(Optional.of(user(21L, role("ROLE_STUDENT"))));
        when(userRepository.findById(22L)).thenReturn(Optional.of(user(22L, role("ROLE_ROOT"))));

        assertDoesNotThrow(() -> service.requireCanManage(20L, 21L));
        assertThrows(ResponseStatusException.class, () -> service.requireCanManage(20L, 22L));
    }

    @Test
    void rootCanManageOtherRoot() {
        final AccessPolicyService service = new AccessPolicyService(userRepository, scopedRoleRepository, classRepository);
        when(userRepository.findById(30L)).thenReturn(Optional.of(user(30L, role("ROLE_ROOT"))));
        when(userRepository.findById(31L)).thenReturn(Optional.of(user(31L, role("ROLE_ROOT"))));

        assertDoesNotThrow(() -> service.requireCanManage(30L, 31L));
    }

    private static RoleEntity role(String name, String... permissionCodes) {
        final Set<PermissionEntity> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissions.add(PermissionEntity.builder().code(code).build());
        }
        return RoleEntity.builder().name(name).permissions(permissions).build();
    }

    private static UserEntity user(Long id, RoleEntity... roles) {
        return UserEntity.builder().id(id).username("u" + id).roles(List.of(roles)).build();
    }

    private static ScopedRoleEntity grant(UserEntity user, RoleEntity role, ScopeType type, Long scopeId) {
        return ScopedRoleEntity.builder().user(user).role(role).scopeType(type).scopeId(scopeId).grantedBy(1L).build();
    }

}