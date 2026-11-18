package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.academic.AcademicCatalogService;
import org.linkwave.userservice.academic.AcademicImportService;
import org.linkwave.userservice.academic.AcademicImportService.ImportReport;
import org.linkwave.userservice.academic.AcademicRecordService;
import org.linkwave.userservice.academic.ManualEntryService;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final AcademicCatalogService catalog;
    private final AcademicRecordService records;
    private final ManualEntryService manual;

    public record TermRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,32}", message = "mã kỳ không hợp lệ") String code,
            @NotBlank @Size(max = 255) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {
    }

    public record FacultyRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,16}", message = "mã khoa không hợp lệ") String code,
            @NotBlank @Size(max = 128) String name
    ) {
    }

    public record ClassRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,32}", message = "mã lớp không hợp lệ") String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull Long facultyId,
            @Size(max = 8) String cohort
    ) {
    }

    @GetMapping("/faculties")
    public List<AcademicCatalogService.FacultyView> faculties() {
        access.require(currentAdminId(), "import.run", SCHOOL, null);
        return catalog.faculties();
    }

    @PostMapping("/faculties")
    @ResponseStatus(NO_CONTENT)
    public void createFaculty(@RequestBody @Valid FacultyRequest body, HttpServletRequest request) {
        saveFaculty(null, body, request);
    }

    @PutMapping("/faculties/{id}")
    @ResponseStatus(NO_CONTENT)
    public void updateFaculty(@PathVariable long id, @RequestBody @Valid FacultyRequest body, HttpServletRequest request) {
        saveFaculty(id, body, request);
    }

    @DeleteMapping("/faculties/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteFaculty(@PathVariable long id, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        catalog.deleteFaculty(id);
        audit.record(actor, "FACULTY_DELETE", "FACULTY", String.valueOf(id), null, ip(request));
    }

    @GetMapping("/classes")
    public List<AcademicCatalogService.ClassView> classes() {
        access.require(currentAdminId(), "import.run", SCHOOL, null);
        return catalog.classes();
    }

    @PostMapping("/classes")
    @ResponseStatus(NO_CONTENT)
    public void createClass(@RequestBody @Valid ClassRequest body, HttpServletRequest request) {
        saveClass(null, body, request);
    }

    @PutMapping("/classes/{id}")
    @ResponseStatus(NO_CONTENT)
    public void updateClass(@PathVariable long id, @RequestBody @Valid ClassRequest body, HttpServletRequest request) {
        saveClass(id, body, request);
    }

    @DeleteMapping("/classes/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteClass(@PathVariable long id, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        catalog.deleteClass(id);
        audit.record(actor, "CLASS_DELETE", "CLASS", String.valueOf(id), null, ip(request));
    }

    /** Tìm kiếm danh sách môn học, TKB, lịch thi, lịch gác thi, học kỳ. q tìm không phân biệt hoa thường. */
    @GetMapping("/records/{kind}")
    public AcademicRecordService.RecordPage records(@PathVariable String kind,
                                                    @RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        access.require(currentAdminId(), "import.run", SCHOOL, null);
        return records.list(AcademicRecordService.parseKind(kind), q, page, size);
    }

    @PostMapping("/records/{kind}")
    @ResponseStatus(NO_CONTENT)
    public void createRecord(@PathVariable String kind, @RequestBody Map<String, String> body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        final AcademicRecordService.Kind parsed = AcademicRecordService.parseKind(kind);
        if (parsed != AcademicRecordService.Kind.SUBJECTS) {
            throw new UnacceptableRequestDataException("Mục này chỉ thêm bằng file Excel");
        }
        records.createSubject(body.get("code"), body.get("name"));
        audit.record(actor, "SUBJECT_CREATE", "SUBJECT", body.get("code"), null, ip(request));
    }

    /** Thêm một dòng bằng form. Khóa của body là tên cột trong file import (ví dụ "MSSV", "Mã GV"). */
    @PostMapping("/manual/{kind}")
    @ResponseStatus(NO_CONTENT)
    public void addManual(@PathVariable String kind, @RequestBody Map<String, String> body, HttpServletRequest request) throws IOException {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        final Kind parsed = parseKind(kind);
        if (!ManualEntryService.supports(parsed)) {
            throw new UnacceptableRequestDataException("Mục này thêm bằng form riêng hoặc chỉ nhập bằng file Excel");
        }
        manual.add(parsed, body);
        audit.record(actor, "MANUAL_ADD_" + parsed.name(), "IMPORT", parsed.name(), null, ip(request));
    }

    @PutMapping("/records/{kind}/{key}")
    @ResponseStatus(NO_CONTENT)
    public void updateRecord(@PathVariable String kind, @PathVariable String key,
                             @RequestBody Map<String, String> body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        final AcademicRecordService.Kind parsed = AcademicRecordService.parseKind(kind);
        records.update(parsed, key, body);
        audit.record(actor, "RECORD_UPDATE", parsed.name(), key, null, ip(request));
    }

    @DeleteMapping("/records/{kind}/{key}")
    @ResponseStatus(NO_CONTENT)
    public void deleteRecord(@PathVariable String kind, @PathVariable String key, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        final AcademicRecordService.Kind parsed = AcademicRecordService.parseKind(kind);
        records.delete(parsed, key);
        audit.record(actor, "RECORD_DELETE", parsed.name(), key, null, ip(request));
    }

    private void saveFaculty(Long id, FacultyRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        catalog.saveFaculty(id, new AcademicCatalogService.FacultyInput(body.code(), body.name()));
        audit.record(actor, id == null ? "FACULTY_CREATE" : "FACULTY_UPDATE", "FACULTY", body.code(), null, ip(request));
    }

    private void saveClass(Long id, ClassRequest body, HttpServletRequest request) {
        final Long actor = currentAdminId();
        access.require(actor, "import.run", SCHOOL, null);
        catalog.saveClass(id, new AcademicCatalogService.ClassInput(body.code(), body.name(), body.facultyId(), body.cohort()));
        audit.record(actor, id == null ? "CLASS_CREATE" : "CLASS_UPDATE", "CLASS", body.code(), null, ip(request));
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
            throw new UnacceptableRequestDataException("Loại import không hợp lệ. Dùng: faculties, classes, subjects, lecturers, students, timetable, exams, duties, terms");
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