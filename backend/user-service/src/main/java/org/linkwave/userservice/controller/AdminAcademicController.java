package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.AcademicImportService;
import org.linkwave.userservice.academic.AcademicImportService.ImportReport;
import org.linkwave.userservice.academic.AcademicImportService.Kind;
import org.linkwave.userservice.academic.AcademicImportService.TermInput;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.service.AccessPolicyService;
import org.linkwave.userservice.service.AuditService;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Nhập dữ liệu học vụ từ Excel và nhập học kỳ bằng form. Cần quyền import.run (ROOT và ADMIN có quyền này).
 * Mặc định dryRun = true: chỉ kiểm tra file và báo lỗi. Gửi dryRun=false để ghi thật.
 */
@RestController
@RequestMapping("/api/v1/admin/academic")
@RequiredArgsConstructor
public class AdminAcademicController {

    private static final ScopeType SCHOOL = ScopeType.SCHOOL;

    private final AccessPolicyService access;
    private final AuditService audit;
    private final AcademicImportService importService;

    public record TermRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,32}", message = "mã kỳ không hợp lệ") String code,
            @NotBlank @Size(max = 255) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {
    }

    @PostMapping(value = "/import/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReport importFile(@PathVariable String kind,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(defaultValue = "true") boolean dryRun,
                                   HttpServletRequest request) throws IOException {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        final Kind parsedKind = parseKind(kind);

        if (file.isEmpty()) {
            throw new UnacceptableRequestDataException("Chưa chọn file");
        }
        final String fileName = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx")) {
            throw new UnacceptableRequestDataException("Chỉ nhận file .xlsx đúng mẫu");
        }

        final ImportReport report;
        try (InputStream in = file.getInputStream()) {
            report = importService.run(parsedKind, in, dryRun);
        }
        if (!dryRun && report.written() > 0) {
            // Nhật ký chỉ ghi số dòng, không ghi mật khẩu tạm
            audit.record(actor, "IMPORT_" + parsedKind.name(), "IMPORT", parsedKind.name(),
                    "rows=" + report.written(), ip(request));
        }
        return report;
    }

    @PostMapping("/terms")
    @ResponseStatus(NO_CONTENT)
    public void saveTerm(@RequestBody @Valid TermRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        importService.saveTerm(new TermInput(body.code(), body.name(), body.startDate(), body.endDate()));
        audit.record(actor, "TERM_SAVE", "TERM", body.code(), null, ip(request));
    }

    private static Kind parseKind(String kind) {
        try {
            return Kind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnacceptableRequestDataException("Loại import không hợp lệ. Dùng: students, timetable, exams, duties, terms");
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

    private static String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}