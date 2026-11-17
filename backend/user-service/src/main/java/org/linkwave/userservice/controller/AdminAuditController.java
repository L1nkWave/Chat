package org.linkwave.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.entity.AuditLogEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.repository.AuditLogRepository;
import org.linkwave.userservice.service.AccessPolicyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/** Nhật ký hệ thống có bộ lọc và phân trang. Yêu cầu quyền audit.view. */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZonedDateTime EARLIEST = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, VN);
    private static final ZonedDateTime LATEST = ZonedDateTime.of(9999, 12, 31, 0, 0, 0, 0, VN);
    private static final int MAX_SIZE = 100;

    private final AccessPolicyService access;
    private final AuditLogRepository auditRepository;

    public record AuditRow(Long id, Long actorId, String action, String targetType, String targetId,
                           String detail, String ip, ZonedDateTime createdAt) {
    }

    public record AuditPage(List<AuditRow> items, long total, int page, int size) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public AuditPage search(@RequestParam(defaultValue = "") String action,
                            @RequestParam(required = false) Long actorId,
                            @RequestParam(defaultValue = "") String targetId,
                            @RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        requireAuditView();

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc");
        }
        final ZonedDateTime fromTime = from == null ? EARLIEST : from.atStartOfDay(VN);
        final ZonedDateTime toTime = to == null ? LATEST : to.plusDays(1).atStartOfDay(VN);
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);

        final Page<AuditLogEntity> result = auditRepository.search(
                action.trim(),
                actorId == null ? 0L : actorId,
                targetId.trim(),
                fromTime,
                toTime,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        final List<AuditRow> items = result.getContent().stream()
                .map(entry -> new AuditRow(
                        entry.getId(),
                        entry.getActorId(),
                        entry.getAction(),
                        entry.getTargetType(),
                        entry.getTargetId(),
                        entry.getDetail(),
                        entry.getIp(),
                        entry.getCreatedAt()))
                .toList();
        return new AuditPage(items, result.getTotalElements(), safePage, safeSize);
    }

    private void requireAuditView() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        final boolean privileged = authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_ROOT");
        if (privileged && !authorities.contains("MFA_VERIFIED")) {
            throw new ResponseStatusException(FORBIDDEN, "Cần bật và xác thực hai lớp trước khi dùng trang quản trị");
        }
        final Long actor = ((DefaultUserDetails) authentication.getPrincipal()).id();
        access.require(actor, "audit.view", SCHOOL, null);
    }
}