package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.FacultyEntity;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.SchoolClassEntity;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AuditService;
import org.linkwave.userservice.service.CatalogService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/** Danh mục khoa và lớp. Xem cần user.view; tạo mới chỉ ROOT. */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;

    private final AccessPolicyService access;
    private final CatalogService catalog;
    private final AuditService audit;

    public record FacultyView(Long id, String code, String name) {
    }

    public record ClassView(Long id, String code, String name, Long facultyId, String facultyName,
                            String cohort, Long advisorId) {
    }

    public record CreateFacultyRequest(
            @NotBlank @Pattern(regexp = "(?i)^[A-Z0-9_]{2,16}$") String code,
            @NotBlank @Size(max = 128) String name) {
    }

    public record CreateClassRequest(
            @NotBlank @Pattern(regexp = "(?i)^[A-Z0-9_.\\-]{2,32}$") String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull Long facultyId,
            @Pattern(regexp = "(?i)^(K\\d{2})?$") String cohort,
            Long advisorId) {
    }

    @GetMapping("/faculties")
    public List<FacultyView> faculties(@RequestParam(defaultValue = "") String q) {
        requireView();
        return catalog.searchFaculties(q).stream()
                .map(f -> new FacultyView(f.getId(), f.getCode(), f.getName()))
                .toList();
    }

    @PostMapping("/faculties")
    @ResponseStatus(CREATED)
    public FacultyView createFaculty(@RequestBody @Valid CreateFacultyRequest body, HttpServletRequest request) {
        final Long actor = requireRoot();
        final FacultyEntity faculty = catalog.createFaculty(body.code(), body.name());
        audit.record(actor, "FACULTY_CREATE", "FACULTY", String.valueOf(faculty.getId()),
                faculty.getCode(), request.getRemoteAddr());
        return new FacultyView(faculty.getId(), faculty.getCode(), faculty.getName());
    }

    @GetMapping("/classes")
    public List<ClassView> classes(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(required = false) Long facultyId) {
        requireView();
        return catalog.searchClasses(q, facultyId).stream()
                .map(CatalogController::toView)
                .toList();
    }

    @PostMapping("/classes")
    @ResponseStatus(CREATED)
    public ClassView createClass(@RequestBody @Valid CreateClassRequest body, HttpServletRequest request) {
        final Long actor = requireRoot();
        final SchoolClassEntity schoolClass = catalog.createClass(
                body.code(), body.name(), body.facultyId(), body.cohort(), body.advisorId());
        audit.record(actor, "CLASS_CREATE", "CLASS", String.valueOf(schoolClass.getId()),
                schoolClass.getCode(), request.getRemoteAddr());
        return toView(schoolClass);
    }

    private static ClassView toView(SchoolClassEntity c) {
        return new ClassView(c.getId(), c.getCode(), c.getName(),
                c.getFaculty().getId(), c.getFaculty().getName(), c.getCohort(), c.getAdvisorId());
    }

    private void requireView() {
        final Long actor = currentActor();
        requireMfaIfPrivileged();
        access.require(actor, "user.view", SCHOOL, null);
    }

    private Long requireRoot() {
        final Long actor = currentActor();
        requireMfaIfPrivileged();
        if (access.levelOf(actor) != RoleEntity.Roles.ROOT.getLevel()) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ROOT được tạo khoa hoặc lớp");
        }
        return actor;
    }

    private static Long currentActor() {
        return ((DefaultUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }

    private static void requireMfaIfPrivileged() {
        final List<String> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new ResponseStatusException(FORBIDDEN, "Cần bật và xác thực hai lớp trước khi dùng trang quản trị");
        }
    }
}