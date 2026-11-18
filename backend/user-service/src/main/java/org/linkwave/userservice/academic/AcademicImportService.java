package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.academic.SpreadsheetReader.RawRow;
import org.linkwave.userservice.academic.SpreadsheetReader.Table;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.RoleCatalogRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.TemporaryPasswords;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhập dữ liệu học vụ từ file .xlsx. Quy tắc chung:
 * - Kiểm tra toàn bộ file trước. Chỉ ghi khi không có dòng lỗi nào (nhập tất cả hoặc không nhập gì).
 * - dryRun = true chỉ kiểm tra và báo lỗi, không ghi.
 * - Nhập lại cùng một file không tạo bản ghi trùng (upsert theo khóa tự nhiên). Báo số dòng thêm mới và số dòng cập nhật.
 * - Thứ tự nhập: khoa -> lớp -> môn học -> giảng viên và sinh viên -> thời khóa biểu, lịch thi, lịch gác thi. Học kỳ độc lập.
 */
@Service
@RequiredArgsConstructor
public class AcademicImportService {

    public enum Kind { FACULTIES, CLASSES, SUBJECTS, LECTURERS, STUDENTS, TIMETABLE, EXAMS, DUTIES, TERMS }

    /** Mật khẩu mặc định cho giảng viên. Bắt buộc đổi ở lần đăng nhập đầu. */
    public static final String DEFAULT_LECTURER_PASSWORD = "Dhsg@123";

    public record RowError(int row, String message) {
    }

    /** Tài khoản mới tạo. Mật khẩu tạm chỉ có trong phản hồi của lần nhập này. */
    public record CreatedAccount(String username, String name, String temporaryPassword) {
    }

    /** inserted: dòng mới. updated: dòng đã có và được cập nhật. written = inserted + updated khi nhập thật. */
    public record ImportReport(String kind, boolean dryRun, int total, int valid, int written,
                               int inserted, int updated,
                               List<RowError> errors, List<CreatedAccount> createdAccounts) {
    }

    public record TermInput(String code, String name, LocalDate startDate, LocalDate endDate) {
    }

    private record Outcome(int valid, int written, List<CreatedAccount> created, int inserted, int updated) {
    }

    private static final Outcome EMPTY = new Outcome(0, 0, List.of(), 0, 0);
    private static final int MAX_REPORTED_ERRORS = 200;
    private static final int MAX_PERIOD = 14;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9._-]{1,32}");
    private static final Pattern MSSV = Pattern.compile("[A-Za-z0-9]{3,16}");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private static final String UPSERT_FACULTY = """
            insert into faculties (code, name, created_at)
            values (?, ?, now())
            on conflict (code) do update set name = excluded.name
            """;

    private static final String UPSERT_CLASS = """
            insert into school_classes (code, name, faculty_id, cohort, created_at)
            values (?, ?, ?, ?, now())
            on conflict (code) do update set
                name = excluded.name,
                faculty_id = excluded.faculty_id,
                cohort = excluded.cohort
            """;

    private static final String UPSERT_SUBJECT = """
            insert into subjects (code, name, created_at)
            values (?, ?, now())
            on conflict (code) do update set name = excluded.name
            """;

    private static final String UPSERT_PROFILE = """
            insert into student_profiles (user_id, student_code, class_code, cccd_enc, cccd_index, updated_at)
            values (?, ?, ?, ?, ?, now())
            on conflict (user_id) do update set
                student_code = excluded.student_code,
                class_code = excluded.class_code,
                cccd_enc = coalesce(excluded.cccd_enc, student_profiles.cccd_enc),
                cccd_index = coalesce(excluded.cccd_index, student_profiles.cccd_index),
                updated_at = now()
            """;

    private static final String UPSERT_SESSION = """
            insert into course_sessions (class_code, subject_code, subject_name, group_code, room, weekday,
                                         start_period, period_count, lecturer_code, lecturer_name, session_date, start_time)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (class_code, subject_code, group_code, session_date, start_period) do update set
                subject_name = excluded.subject_name,
                room = excluded.room,
                weekday = excluded.weekday,
                period_count = excluded.period_count,
                lecturer_code = excluded.lecturer_code,
                lecturer_name = excluded.lecturer_name,
                start_time = excluded.start_time
            """;

    private static final String UPSERT_EXAM = """
            insert into exam_schedules (subject_code, subject_name, candidate_count, exam_date, exam_time, room, campus)
            values (?, ?, ?, ?, ?, ?, ?)
            on conflict (subject_code, exam_date, exam_time, room) do update set
                subject_name = excluded.subject_name,
                candidate_count = excluded.candidate_count,
                campus = excluded.campus
            """;

    private static final String UPSERT_DUTY = """
            insert into exam_duties (lecturer_code, subject_code, exam_date, exam_time, room, campus)
            values (?, ?, ?, ?, ?, ?)
            on conflict (lecturer_code, exam_date, exam_time, room) do update set
                subject_code = excluded.subject_code,
                campus = excluded.campus
            """;

    private static final String UPSERT_TERM = """
            insert into academic_terms (code, name, start_date, end_date)
            values (?, ?, ?, ?)
            on conflict (code) do update set
                name = excluded.name,
                start_date = excluded.start_date,
                end_date = excluded.end_date
            """;

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final RoleCatalogRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CccdCipher cipher;

    @Transactional
    public ImportReport run(Kind kind, InputStream in, boolean dryRun) throws IOException {
        final Table table = SpreadsheetReader.read(in);
        final List<RowError> errors = new ArrayList<>();
        final Outcome outcome = switch (kind) {
            case FACULTIES -> faculties(table, dryRun, errors);
            case CLASSES -> classes(table, dryRun, errors);
            case SUBJECTS -> subjects(table, dryRun, errors);
            case LECTURERS -> lecturers(table, dryRun, errors);
            case STUDENTS -> students(table, dryRun, errors);
            case TIMETABLE -> timetable(table, dryRun, errors);
            case EXAMS -> exams(table, dryRun, errors);
            case DUTIES -> duties(table, dryRun, errors);
            case TERMS -> terms(table, dryRun, errors);
        };
        return new ImportReport(kind.name(), dryRun, table.rows().size(), outcome.valid(), outcome.written(),
                outcome.inserted(), outcome.updated(),
                errors.stream().limit(MAX_REPORTED_ERRORS).toList(), outcome.created());
    }

    /** Thêm hoặc sửa một học kỳ từ form. Trả lỗi nếu dữ liệu không hợp lệ hoặc khoảng ngày trùng học kỳ khác. */
    public void saveTerm(TermInput term) {
        final String problem = termProblem(term.code(), term.name(), term.startDate(), term.endDate());
        if (problem != null) {
            throw new UnacceptableRequestDataException(problem);
        }
        final String overlap = overlapInDatabase(term.code(), term.startDate(), term.endDate());
        if (overlap != null) {
            throw new UnacceptableRequestDataException(overlap);
        }
        jdbc.update(UPSERT_TERM, term.code(), term.name().trim(), term.startDate(), term.endDate());
    }

    // ---------------------------------------------------------------- khoa

    private record FacultyRow(int row, String code, String name) {
    }

    private Outcome faculties(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã khoa", "Tên khoa")) {
            return EMPTY;
        }
        final List<FacultyRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String code = raw.get("Mã khoa");
            final String name = raw.get("Tên khoa");
            if (!CODE.matcher(code).matches()) {
                problem(errors, raw.number(), "Mã khoa không hợp lệ (chữ, số, dấu . _ -, tối đa 16 ký tự)");
            } else if (code.length() > 16) {
                problem(errors, raw.number(), "Mã khoa quá dài");
            } else if (!seen.add(code.toUpperCase(Locale.ROOT))) {
                problem(errors, raw.number(), "Mã khoa " + code + " bị trùng trong file");
            }
            if (name.isEmpty() || name.length() > 128) {
                problem(errors, raw.number(), "Tên khoa trống hoặc dài quá 128 ký tự");
            }
            if (errors.size() == before) {
                rows.add(new FacultyRow(raw.number(), code, name));
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from faculties where code = ?", r.code()))
                .count();
        jdbc.batchUpdate(UPSERT_FACULTY, rows.stream().map(r -> new Object[]{r.code(), r.name()}).toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- lớp

    private record ClassRow(int row, String code, String name, long facultyId, String cohort) {
    }

    private Outcome classes(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã lớp", "Tên lớp", "Mã khoa")) {
            return EMPTY;
        }
        final List<RawRow> raws = table.rows();
        final Map<String, Long> faculties = facultyIdsByCode(raws.stream()
                .map(r -> r.get("Mã khoa"))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList());

        final List<ClassRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : raws) {
            final int before = errors.size();
            final String code = raw.get("Mã lớp");
            final String name = raw.get("Tên lớp");
            final String facultyCode = raw.get("Mã khoa");
            final String cohort = blankToNull(raw.get("Niên khóa"));

            if (!CODE.matcher(code).matches()) {
                problem(errors, raw.number(), "Mã lớp không hợp lệ (ví dụ DCT1222)");
            } else if (!seen.add(code.toUpperCase(Locale.ROOT))) {
                problem(errors, raw.number(), "Mã lớp " + code + " bị trùng trong file");
            }
            if (name.isEmpty() || name.length() > 128) {
                problem(errors, raw.number(), "Tên lớp trống hoặc dài quá 128 ký tự");
            }
            final Long facultyId = faculties.get(facultyCode);
            if (facultyId == null) {
                problem(errors, raw.number(), "Khoa " + facultyCode + " chưa có trong danh mục. Nhập file Khoa trước");
            }
            if (cohort != null && cohort.length() > 8) {
                problem(errors, raw.number(), "Niên khóa quá dài (tối đa 8 ký tự, ví dụ K23)");
            }
            if (errors.size() == before) {
                rows.add(new ClassRow(raw.number(), code, name, facultyId, cohort));
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from school_classes where code = ?", r.code()))
                .count();
        jdbc.batchUpdate(UPSERT_CLASS, rows.stream()
                .map(r -> new Object[]{r.code(), r.name(), r.facultyId(), r.cohort()})
                .toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- môn học

    private record SubjectRow(int row, String code, String name) {
    }

    private Outcome subjects(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã MH", "Tên môn học|Môn")) {
            return EMPTY;
        }
        final List<SubjectRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String code = raw.get("Mã MH");
            final String name = raw.get("Tên môn học", "Môn");
            if (!CODE.matcher(code).matches()) {
                problem(errors, raw.number(), "Mã MH không hợp lệ (chữ, số, dấu . _ -, tối đa 32 ký tự)");
            } else if (!seen.add(code.toUpperCase(Locale.ROOT))) {
                problem(errors, raw.number(), "Mã MH " + code + " bị trùng trong file");
            }
            if (name.isEmpty() || name.length() > 255) {
                problem(errors, raw.number(), "Tên môn học trống hoặc dài quá 255 ký tự");
            }
            if (errors.size() == before) {
                rows.add(new SubjectRow(raw.number(), code, name));
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from subjects where code = ?", r.code()))
                .count();
        jdbc.batchUpdate(UPSERT_SUBJECT, rows.stream().map(r -> new Object[]{r.code(), r.name()}).toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- giảng viên

    private record LecturerRow(int row, String code, String name) {
    }

    private Outcome lecturers(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã GV", "Họ tên")) {
            return EMPTY;
        }
        final List<LecturerRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final Map<String, Long> existing = usersByUsername(table.rows().stream()
                .map(r -> r.get("Mã GV"))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList());
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String code = raw.get("Mã GV");
            final String name = raw.get("Họ tên");
            if (!CODE.matcher(code).matches()) {
                problem(errors, raw.number(), "Mã GV không hợp lệ (đây là tên đăng nhập của giảng viên)");
            } else if (!seen.add(code.toUpperCase(Locale.ROOT))) {
                problem(errors, raw.number(), "Mã GV " + code + " bị trùng trong file");
            } else if (existing.containsKey(code) && !isLecturer(existing.get(code))) {
                problem(errors, raw.number(), "Mã GV " + code + " đã là tài khoản không phải giảng viên");
            }
            if (name.isEmpty() || name.length() > 64) {
                problem(errors, raw.number(), "Họ tên trống hoặc dài quá 64 ký tự");
            }
            if (errors.size() == before) {
                rows.add(new LecturerRow(raw.number(), code, name));
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final RoleEntity baseRole = role("ROLE_USER");
        final RoleEntity lecturerRole = role("ROLE_LECTURER");
        final List<CreatedAccount> created = new ArrayList<>();
        int updated = 0;
        for (LecturerRow r : rows) {
            final Long userId = existing.get(r.code());
            if (userId == null) {
                userRepository.save(UserEntity.builder()
                        .username(r.code())
                        .password(passwordEncoder.encode(DEFAULT_LECTURER_PASSWORD))
                        .name(r.name())
                        .roles(new ArrayList<>(List.of(baseRole, lecturerRole)))
                        .mustChangePassword(true)
                        .build());
                created.add(new CreatedAccount(r.code(), r.name(), DEFAULT_LECTURER_PASSWORD));
            } else {
                jdbc.update("update users set name = ? where id = ?", r.name(), userId);
                updated++;
            }
        }
        return new Outcome(rows.size(), rows.size(), created, created.size(), updated);
    }

    // ---------------------------------------------------------------- sinh viên

    private record StudentRow(int row, String mssv, String name, String classCode, String cccd) {
    }

    private Outcome students(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "MSSV", "Họ tên", "Lớp")) {
            return EMPTY;
        }
        // Lớp phải đã có trong danh mục (nhập file Khoa và Lớp trước). Không tự tạo lớp.
        final Set<String> knownClasses = classCodesIn(table.rows().stream()
                .map(r -> r.get("Lớp"))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList());
        final List<StudentRow> rows = new ArrayList<>();
        final Set<String> mssvSeen = new HashSet<>();
        final Set<String> cccdSeen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String mssv = raw.get("MSSV");
            final String name = raw.get("Họ tên");
            final String classCode = raw.get("Lớp");
            final String cccd = raw.get("CCCD");

            if (!MSSV.matcher(mssv).matches()) {
                problem(errors, raw.number(), "MSSV phải gồm 3-16 chữ hoặc số");
            } else if (!mssvSeen.add(mssv.toUpperCase(Locale.ROOT))) {
                problem(errors, raw.number(), "MSSV " + mssv + " bị trùng trong file");
            }
            if (name.isEmpty() || name.length() > 64) {
                problem(errors, raw.number(), "Họ tên trống hoặc dài quá 64 ký tự");
            }
            if (!CODE.matcher(classCode).matches()) {
                problem(errors, raw.number(), "Lớp không hợp lệ (mã lớp, ví dụ DCT1222)");
            } else if (!knownClasses.contains(classCode)) {
                problem(errors, raw.number(), "Lớp " + classCode + " chưa có trong danh mục. Nhập file Khoa và Lớp trước");
            }
            if (!cccd.isEmpty()) {
                if (!CccdCipher.isValid(cccd)) {
                    problem(errors, raw.number(), "CCCD phải đủ 12 chữ số. Nếu mất số 0 đầu, hãy định dạng cột là Text rồi nhập lại");
                } else if (!cccdSeen.add(cccd)) {
                    problem(errors, raw.number(), "CCCD bị trùng trong file");
                }
            }
            if (errors.size() == before) {
                rows.add(new StudentRow(raw.number(), mssv, name, classCode, cccd.isEmpty() ? null : cccd));
            }
        }

        // Kiểm tra với dữ liệu hiện có: CCCD đã thuộc sinh viên khác thì không được gán lại
        final Map<String, Long> existing = usersByUsername(rows.stream().map(StudentRow::mssv).toList());
        final Map<String, Long> cccdOwners = cccdOwners(rows.stream()
                .filter(r -> r.cccd() != null)
                .map(r -> cipher.index(r.cccd()))
                .toList());
        for (StudentRow r : rows) {
            if (r.cccd() == null) {
                continue;
            }
            final Long owner = cccdOwners.get(cipher.index(r.cccd()));
            if (owner != null && !owner.equals(existing.get(r.mssv()))) {
                problem(errors, r.row(), "CCCD này đã thuộc một sinh viên khác");
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }

        final RoleEntity baseRole = role("ROLE_USER");
        final RoleEntity studentRole = role("ROLE_STUDENT");
        final List<CreatedAccount> created = new ArrayList<>();
        int updated = 0;
        for (StudentRow r : rows) {
            Long userId = existing.get(r.mssv());
            if (userId == null) {
                final String temporary = TemporaryPasswords.generate(r.mssv());
                userId = userRepository.save(UserEntity.builder()
                        .username(r.mssv())
                        .password(passwordEncoder.encode(temporary))
                        .name(r.name())
                        .roles(new ArrayList<>(List.of(baseRole, studentRole)))
                        .mustChangePassword(true)
                        .build()).getId();
                created.add(new CreatedAccount(r.mssv(), r.name(), temporary));
            } else {
                jdbc.update("update users set name = ? where id = ?", r.name(), userId);
                updated++;
            }
            jdbc.update(UPSERT_PROFILE, userId, r.mssv(), r.classCode(),
                    r.cccd() == null ? null : cipher.encrypt(r.cccd()),
                    r.cccd() == null ? null : cipher.index(r.cccd()));
        }
        return new Outcome(rows.size(), rows.size(), created, created.size(), updated);
    }

    // ---------------------------------------------------------------- thời khóa biểu

    private record SessionRow(int row, String classCode, String subjectCode, String subjectName, String groupCode,
                              String room, int weekday, int startPeriod, int periodCount, String lecturerCode,
                              String lecturerName, LocalDate date, LocalTime time) {
    }

    private Outcome timetable(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã MH", "Lớp", "Thứ", "Tiết", "Số tiết", "Ngày", "Giờ bắt đầu")) {
            return EMPTY;
        }
        // Môn phải đã có trong danh mục Môn học (nhập file Môn học trước)
        final Set<String> knownSubjects = subjectCodesIn(table.rows().stream()
                .map(r -> r.get("Mã MH"))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList());
        final List<SessionRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String subject = raw.get("Mã MH");
            final String classCode = raw.get("Lớp");
            final String group = raw.get("Nhóm");
            // GV trên thời khóa biểu là họ tên giảng viên. Cột "Mã GV" (nếu có) là tên đăng nhập.
            final String lecturerName = raw.get("GV");
            final String lecturerCode = raw.get("Mã GV");
            final Integer weekday = parseWeekday(raw.get("Thứ"));
            final Integer period = parseInt(raw.get("Tiết"));
            final Integer count = parseInt(raw.get("Số tiết"));
            final LocalDate date = parseDate(raw.get("Ngày"));
            final LocalTime time = parseTime(raw.get("Giờ bắt đầu"));

            if (!CODE.matcher(subject).matches()) {
                problem(errors, raw.number(), "Mã MH không hợp lệ");
            }
            if (!CODE.matcher(classCode).matches()) {
                problem(errors, raw.number(), "Lớp không hợp lệ");
            }
            if (CODE.matcher(subject).matches() && !knownSubjects.contains(subject)) {
                problem(errors, raw.number(), "Môn " + subject + " chưa có trong danh mục. Nhập file Môn học trước");
            }
            if (group.length() > 32) {
                problem(errors, raw.number(), "Nhóm quá dài");
            }
            if (lecturerName.length() > 255) {
                problem(errors, raw.number(), "Tên GV quá dài");
            }
            if (!lecturerCode.isEmpty() && !CODE.matcher(lecturerCode).matches()) {
                problem(errors, raw.number(), "Mã GV không hợp lệ");
            }
            if (weekday == null) {
                problem(errors, raw.number(), "Thứ phải là 2 đến 7, hoặc CN");
            }
            if (period == null || period < 1 || period > MAX_PERIOD) {
                problem(errors, raw.number(), "Tiết phải từ 1 đến " + MAX_PERIOD);
            }
            if (count == null || count < 1 || period == null || period + count - 1 > MAX_PERIOD) {
                problem(errors, raw.number(), "Số tiết không hợp lệ");
            }
            if (date == null) {
                problem(errors, raw.number(), "Ngày phải dạng dd/mm/yyyy");
            }
            if (time == null) {
                problem(errors, raw.number(), "Giờ bắt đầu phải dạng HH:mm");
            }
            if (errors.size() == before) {
                final String key = classCode + "|" + subject + "|" + group + "|" + date + "|" + period;
                if (seen.add(key)) {
                    rows.add(new SessionRow(raw.number(), classCode, subject, blankToNull(raw.get("Môn")),
                            group, blankToNull(roomOf(raw.get("Phòng"))), weekday, period, count,
                            blankToNull(lecturerCode), blankToNull(lecturerName), date, time));
                } else {
                    problem(errors, raw.number(), "Dòng trùng với dòng khác trong file");
                }
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from course_sessions where class_code = ? and subject_code = ?"
                                + " and group_code = ? and session_date = ? and start_period = ?",
                        r.classCode(), r.subjectCode(), r.groupCode(), r.date(), r.startPeriod()))
                .count();
        jdbc.batchUpdate(UPSERT_SESSION, rows.stream()
                .map(r -> new Object[]{
                        r.classCode(), r.subjectCode(), r.subjectName(), r.groupCode(), r.room(), r.weekday(),
                        r.startPeriod(), r.periodCount(), r.lecturerCode(), r.lecturerName(), r.date(), r.time()})
                .toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- lịch thi

    private record ExamRow(int row, String subject, String subjectName, Integer candidates, LocalDate date,
                           LocalTime time, String room, String campus) {
    }

    private Outcome exams(Table table, boolean dryRun, List<RowError> errors) {
        // Tên cột theo file lịch thi của trường; tên cũ (Ngày, Giờ thi, Phòng, Số thí sinh) vẫn được nhận
        if (!hasColumns(table, errors, "Mã MH", "Ngày thi|Ngày", "Giờ bắt đầu|Giờ thi", "Phòng thi|Phòng")) {
            return EMPTY;
        }
        final Set<String> knownSubjects = subjectCodesIn(table.rows().stream()
                .map(r -> r.get("Mã MH"))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList());
        final List<ExamRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String subject = raw.get("Mã MH");
            final String room = roomOf(raw.get("Phòng thi", "Phòng"));
            final String candidateText = raw.get("Sĩ số", "Số thí sinh");
            final Integer candidates = candidateText.isEmpty() ? null : parseInt(candidateText);
            final LocalDate date = parseDate(raw.get("Ngày thi", "Ngày"));
            final LocalTime time = parseTime(raw.get("Giờ bắt đầu", "Giờ thi"));

            if (!CODE.matcher(subject).matches()) {
                problem(errors, raw.number(), "Mã MH không hợp lệ");
            }
            if (CODE.matcher(subject).matches() && !knownSubjects.contains(subject)) {
                problem(errors, raw.number(), "Môn " + subject + " chưa có trong danh mục. Nhập file Môn học trước");
            }
            if (room.isEmpty() || room.length() > 32) {
                problem(errors, raw.number(), "Phòng thi trống hoặc quá dài");
            }
            if (!candidateText.isEmpty() && (candidates == null || candidates < 0)) {
                problem(errors, raw.number(), "Sĩ số phải là số");
            }
            if (date == null) {
                problem(errors, raw.number(), "Ngày thi phải dạng dd/mm/yyyy");
            }
            if (time == null) {
                problem(errors, raw.number(), "Giờ bắt đầu phải dạng HH:mm");
            }
            final String campus = campusOf(raw.get("Cơ sở"), room);
            if (campus != null && campus.length() > 8) {
                problem(errors, raw.number(), "Cơ sở quá dài");
            }
            if (errors.size() == before) {
                final String key = subject + "|" + date + "|" + time + "|" + room;
                if (seen.add(key)) {
                    rows.add(new ExamRow(raw.number(), subject, blankToNull(raw.get("Tên môn học", "Tên môn")), candidates,
                            date, time, room, campus));
                } else {
                    problem(errors, raw.number(), "Dòng trùng với dòng khác trong file");
                }
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from exam_schedules where subject_code = ? and exam_date = ?"
                        + " and exam_time = ? and room = ?", r.subject(), r.date(), r.time(), r.room()))
                .count();
        jdbc.batchUpdate(UPSERT_EXAM, rows.stream()
                .map(r -> new Object[]{r.subject(), r.subjectName(), r.candidates(), r.date(), r.time(), r.room(), r.campus()})
                .toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- lịch gác thi

    private record DutyRow(int row, String lecturer, String subject, LocalDate date, LocalTime time,
                           String room, String campus) {
    }

    private Outcome duties(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã GV", "Ngày|Ngày thi", "Giờ thi|Giờ bắt đầu", "Phòng|Phòng thi")) {
            return EMPTY;
        }
        final List<DutyRow> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final int before = errors.size();
            final String lecturer = raw.get("Mã GV");
            final String room = roomOf(raw.get("Phòng", "Phòng thi"));
            final String subject = raw.get("Mã MH");
            final LocalDate date = parseDate(raw.get("Ngày", "Ngày thi"));
            final LocalTime time = parseTime(raw.get("Giờ thi", "Giờ bắt đầu"));

            if (!CODE.matcher(lecturer).matches()) {
                problem(errors, raw.number(), "Mã GV không hợp lệ");
            }
            if (room.isEmpty() || room.length() > 32) {
                problem(errors, raw.number(), "Phòng trống hoặc quá dài");
            }
            if (!subject.isEmpty() && !CODE.matcher(subject).matches()) {
                problem(errors, raw.number(), "Mã MH không hợp lệ");
            }
            if (date == null) {
                problem(errors, raw.number(), "Ngày phải dạng dd/mm/yyyy");
            }
            if (time == null) {
                problem(errors, raw.number(), "Giờ thi phải dạng HH:mm");
            }
            final String campus = campusOf(raw.get("Cơ sở"), room);
            if (errors.size() == before) {
                final String key = lecturer + "|" + date + "|" + time + "|" + room;
                if (seen.add(key)) {
                    rows.add(new DutyRow(raw.number(), lecturer, blankToNull(subject), date, time, room, campus));
                } else {
                    problem(errors, raw.number(), "Dòng trùng với dòng khác trong file");
                }
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from exam_duties where lecturer_code = ? and exam_date = ?"
                        + " and exam_time = ? and room = ?", r.lecturer(), r.date(), r.time(), r.room()))
                .count();
        jdbc.batchUpdate(UPSERT_DUTY, rows.stream()
                .map(r -> new Object[]{r.lecturer(), r.subject(), r.date(), r.time(), r.room(), r.campus()})
                .toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- học kỳ

    private Outcome terms(Table table, boolean dryRun, List<RowError> errors) {
        if (!hasColumns(table, errors, "Mã kỳ", "Tên học kỳ", "Ngày bắt đầu", "Ngày kết thúc")) {
            return EMPTY;
        }
        final List<TermInput> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (RawRow raw : table.rows()) {
            final String code = raw.get("Mã kỳ");
            final String name = raw.get("Tên học kỳ");
            final LocalDate start = parseDate(raw.get("Ngày bắt đầu"));
            final LocalDate end = parseDate(raw.get("Ngày kết thúc"));
            final String problem = termProblem(code, name, start, end);
            if (problem != null) {
                problem(errors, raw.number(), problem);
            } else if (!seen.add(code)) {
                problem(errors, raw.number(), "Mã kỳ bị trùng trong file");
            } else {
                // Khoảng ngày không được trùng học kỳ khác (trong file hoặc đã có trong hệ thống)
                final String overlap = rows.stream()
                        .filter(other -> !other.code().equals(code) && overlaps(other.startDate(), other.endDate(), start, end))
                        .map(other -> "Khoảng ngày trùng với học kỳ " + other.code() + " trong file")
                        .findFirst()
                        .orElseGet(() -> overlapInDatabase(code, start, end));
                if (overlap != null) {
                    problem(errors, raw.number(), overlap);
                } else {
                    rows.add(new TermInput(code, name, start, end));
                }
            }
        }

        if (dryRun || !errors.isEmpty()) {
            return dry(rows.size());
        }
        final int updated = (int) rows.stream()
                .filter(r -> exists("select count(*) from academic_terms where code = ?", r.code()))
                .count();
        jdbc.batchUpdate(UPSERT_TERM, rows.stream()
                .map(r -> new Object[]{r.code(), r.name(), r.startDate(), r.endDate()})
                .toList());
        return saved(rows.size(), updated);
    }

    // ---------------------------------------------------------------- tiện ích

    private static Outcome dry(int valid) {
        return new Outcome(valid, 0, List.of(), 0, 0);
    }

    /** updated dòng đã có, phần còn lại là dòng mới */
    private static Outcome saved(int count, int updated) {
        return new Outcome(count, count, List.of(), count - updated, updated);
    }

    private boolean exists(String sql, Object... args) {
        final Long count = jdbc.queryForObject(sql, Long.class, args);
        return count != null && count > 0;
    }

    private static boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aStart.isAfter(bEnd) && !bStart.isAfter(aEnd);
    }

    /** Tên học kỳ khác có khoảng ngày chồng lên khoảng ngày này, hoặc null nếu không trùng */
    private String overlapInDatabase(String code, LocalDate start, LocalDate end) {
        return jdbc.query("select code from academic_terms where code <> ? and start_date <= ? and end_date >= ? limit 1",
                        (rs, n) -> rs.getString(1), code, end, start)
                .stream()
                .findFirst()
                .map(other -> "Khoảng ngày trùng với học kỳ " + other + " đã có trong hệ thống")
                .orElse(null);
    }

    private boolean isLecturer(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream().anyMatch(role -> "ROLE_LECTURER".equals(role.getName())))
                .orElse(false);
    }

    private static String termProblem(String code, String name, LocalDate start, LocalDate end) {
        if (!CODE.matcher(code).matches()) {
            return "Mã kỳ không hợp lệ (chữ, số, dấu . _ -, tối đa 32 ký tự)";
        }
        if (name.isEmpty() || name.length() > 255) {
            return "Tên học kỳ trống hoặc quá dài";
        }
        if (start == null || end == null) {
            return "Ngày bắt đầu và ngày kết thúc phải dạng dd/mm/yyyy";
        }
        if (end.isBefore(start)) {
            return "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu";
        }
        return null;
    }

    /** Mỗi tham số là một cột, hoặc nhiều tên cách nhau bằng "|" (chỉ cần có một tên trong file) */
    private boolean hasColumns(Table table, List<RowError> errors, String... names) {
        final List<String> missing = Arrays.stream(names)
                .filter(group -> Arrays.stream(group.split("\\|"))
                        .noneMatch(alias -> table.headers().contains(SpreadsheetReader.normalize(alias))))
                .toList();
        if (!missing.isEmpty()) {
            errors.add(new RowError(1, "File thiếu cột: " + String.join(", ", missing).replace("|", " hoặc ")));
        }
        return missing.isEmpty();
    }

    private static void problem(List<RowError> errors, int row, String message) {
        errors.add(new RowError(row, message));
    }

    /** Mã phòng là phần trước dấu "-": "C.E603-Phòng học Cơ sở Chính" -> "C.E603" */
    private static String roomOf(String text) {
        final String trimmed = text == null ? "" : text.trim();
        final int dash = trimmed.indexOf('-');
        return dash > 0 ? trimmed.substring(0, dash).trim() : trimmed;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    /** Thứ 2..7 hoặc CN (trả về 8). Chấp nhận "2", "Thứ 2", "CN". */
    private static Integer parseWeekday(String text) {
        final String normalized = SpreadsheetReader.normalize(text);
        if (normalized.equals("cn")) {
            return 8;
        }
        final Matcher m = Pattern.compile("(?:thu)?([2-7])").matcher(normalized);
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim(), TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Cơ sở lấy từ cột "Cơ sở"; nếu trống thì lấy tiền tố của phòng (C.A103 -> C, 1.C305 -> 1) */
    private static String campusOf(String cell, String room) {
        if (!cell.isBlank()) {
            return cell.trim();
        }
        final int dot = room.indexOf('.');
        return dot > 0 ? room.substring(0, dot) : null;
    }

    private Map<String, Long> usersByUsername(List<String> usernames) {
        final Map<String, Long> found = new HashMap<>();
        if (usernames.isEmpty()) {
            return found;
        }
        jdbc.query("select username, id from users where username in (" + placeholders(usernames.size()) + ")",
                        (rs, n) -> Map.entry(rs.getString(1), rs.getLong(2)), usernames.toArray())
                .forEach(e -> found.put(e.getKey(), e.getValue()));
        return found;
    }

    private Map<String, Long> facultyIdsByCode(List<String> codes) {
        final Map<String, Long> found = new HashMap<>();
        if (codes.isEmpty()) {
            return found;
        }
        jdbc.query("select code, id from faculties where code in (" + placeholders(codes.size()) + ")",
                        (rs, n) -> Map.entry(rs.getString(1), rs.getLong(2)), codes.toArray())
                .forEach(e -> found.put(e.getKey(), e.getValue()));
        return found;
    }

    private Set<String> subjectCodesIn(List<String> codes) {
        final Set<String> found = new HashSet<>();
        if (codes.isEmpty()) {
            return found;
        }
        jdbc.query("select code from subjects where code in (" + placeholders(codes.size()) + ")",
                        (rs, n) -> rs.getString(1), codes.toArray())
                .forEach(found::add);
        return found;
    }

    private Set<String> classCodesIn(List<String> codes) {
        final Set<String> found = new HashSet<>();
        if (codes.isEmpty()) {
            return found;
        }
        jdbc.query("select code from school_classes where code in (" + placeholders(codes.size()) + ")",
                        (rs, n) -> rs.getString(1), codes.toArray())
                .forEach(found::add);
        return found;
    }

    private Map<String, Long> cccdOwners(List<String> indexes) {
        final Map<String, Long> owners = new HashMap<>();
        if (indexes.isEmpty()) {
            return owners;
        }
        jdbc.query("select cccd_index, user_id from student_profiles where cccd_index in (" + placeholders(indexes.size()) + ")",
                        (rs, n) -> Map.entry(rs.getString(1), rs.getLong(2)), indexes.toArray())
                .forEach(e -> owners.put(e.getKey(), e.getValue()));
        return owners;
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private RoleEntity role(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Thiếu vai trò " + name + " (chạy db/phase3-p1-roles.sql)"));
    }
}