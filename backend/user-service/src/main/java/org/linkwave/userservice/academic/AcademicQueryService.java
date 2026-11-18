package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Truy vấn dữ liệu học vụ cho bot. Sinh viên xem theo lớp của mình.
 * Giảng viên xem theo mã đăng nhập hoặc họ tên (trên thời khóa biểu, cột GV là họ tên).
 * Mọi truy vấn đều suy ra phạm vi từ người dùng, không nhận mã lớp hay mã giảng viên từ bên ngoài.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicQueryService {

    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_RANGE_DAYS = 62;

    public record SessionView(LocalDate date, int startPeriod, int periodCount, LocalTime startTime,
                              String subjectCode, String subjectName, String groupCode, String room,
                              String classCode, String lecturerCode) {
    }

    public record ExamView(LocalDate date, LocalTime time, String subjectCode, String subjectName,
                           String room, String campus) {
    }

    public record DutyView(LocalDate date, LocalTime time, String subjectCode, String subjectName,
                           String room, String campus) {
    }

    /** classmates chỉ có với sinh viên; với giảng viên là null */
    public record ClassView(String classCode, Integer classmates) {
    }

    public record TermView(String code, String name, LocalDate startDate, LocalDate endDate) {
    }

    /**
     * Phạm vi của người dùng. Sinh viên: classCode. Giảng viên: lecturerCode (tên đăng nhập) và lecturerName (họ tên).
     * Cả ba null nghĩa là không có dữ liệu học vụ.
     */
    private record Scope(String classCode, String lecturerCode, String lecturerName) {
        boolean lecturer() {
            return lecturerCode != null;
        }

        boolean empty() {
            return classCode == null && lecturerCode == null;
        }

        /** Điều kiện lọc course_sessions. Giảng viên khớp theo mã hoặc theo họ tên (không phân biệt hoa thường). */
        String filter() {
            return lecturer()
                    ? "(lecturer_code = ? or lower(trim(lecturer_name)) = lower(trim(?)))"
                    : "class_code = ?";
        }

        Object[] filterParams() {
            return lecturer() ? new Object[]{lecturerCode, lecturerName == null ? "" : lecturerName} : new Object[]{classCode};
        }
    }

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;

    public List<SessionView> timetable(Long userId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        }
        final Scope scope = scopeOf(userId);
        if (scope.empty()) {
            return List.of();
        }
        final List<Object> params = new ArrayList<>(Arrays.asList(scope.filterParams()));
        params.add(from);
        params.add(to);
        return jdbc.query("""
                        select session_date, start_period, period_count, start_time, subject_code, subject_name,
                               group_code, room, class_code, lecturer_code
                        from course_sessions
                        where %s and session_date between ? and ?
                        order by session_date, start_period
                        """.formatted(scope.filter()),
                (rs, n) -> new SessionView(
                        rs.getObject("session_date", LocalDate.class),
                        rs.getInt("start_period"),
                        rs.getInt("period_count"),
                        rs.getObject("start_time", LocalTime.class),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("group_code"),
                        rs.getString("room"),
                        rs.getString("class_code"),
                        rs.getString("lecturer_code")),
                params.toArray());
    }

    /** Lịch thi sắp tới của các môn trong thời khóa biểu của người dùng */
    public List<ExamView> exams(Long userId) {
        final Scope scope = scopeOf(userId);
        final List<String> subjects = subjectCodes(scope);
        if (subjects.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                        select exam_date, exam_time, subject_code, subject_name, room, campus
                        from exam_schedules
                        where subject_code in (%s) and exam_date >= ?
                        order by exam_date, exam_time
                        """.formatted(placeholders(subjects.size())),
                (rs, n) -> new ExamView(
                        rs.getObject("exam_date", LocalDate.class),
                        rs.getObject("exam_time", LocalTime.class),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("room"),
                        rs.getString("campus")),
                concat(subjects, today()).toArray());
    }

    /** Lịch gác thi sắp tới. Chỉ giảng viên có dữ liệu. */
    public List<DutyView> duties(Long userId) {
        final Scope scope = scopeOf(userId);
        if (!scope.lecturer()) {
            return List.of();
        }
        return jdbc.query("""
                        select d.exam_date, d.exam_time, d.subject_code,
                               coalesce(
                                   (select s.subject_name from course_sessions s
                                     where s.subject_code = d.subject_code and s.subject_name is not null limit 1),
                                   (select e.subject_name from exam_schedules e
                                     where e.subject_code = d.subject_code and e.subject_name is not null limit 1)
                               ) as subject_name,
                               d.room, d.campus
                        from exam_duties d
                        where d.lecturer_code = ? and d.exam_date >= ?
                        order by d.exam_date, d.exam_time
                        """,
                (rs, n) -> new DutyView(
                        rs.getObject("exam_date", LocalDate.class),
                        rs.getObject("exam_time", LocalTime.class),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("room"),
                        rs.getString("campus")),
                scope.lecturerCode(), today());
    }

    /** Sinh viên: lớp và số sinh viên cùng lớp. Giảng viên: các lớp đang dạy. */
    public List<ClassView> classes(Long userId) {
        final Scope scope = scopeOf(userId);
        if (scope.lecturer()) {
            return jdbc.query("select distinct class_code from course_sessions where " + scope.filter() + " order by class_code",
                    (rs, n) -> new ClassView(rs.getString(1), null), scope.filterParams());
        }
        if (scope.classCode() == null) {
            return List.of();
        }
        final Integer count = jdbc.queryForObject(
                "select count(*) from student_profiles where class_code = ?", Integer.class, scope.classCode());
        return List.of(new ClassView(scope.classCode(), count));
    }

    /** Học kỳ có chứa ngày này */
    public List<TermView> termsAt(LocalDate date) {
        return jdbc.query("select code, name, start_date, end_date from academic_terms where ? between start_date and end_date order by start_date",
                (rs, n) -> new TermView(rs.getString("code"), rs.getString("name"),
                        rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class)),
                date);
    }

    private Scope scopeOf(Long userId) {
        final UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new Scope(null, null, null);
        }
        final boolean lecturer = user.getRoles().stream().anyMatch(r -> "ROLE_LECTURER".equals(r.getName()));
        if (lecturer) {
            return new Scope(null, user.getUsername(), user.getName());
        }
        final List<String> classes = jdbc.query("select class_code from student_profiles where user_id = ?",
                (rs, n) -> rs.getString(1), userId);
        return new Scope(classes.isEmpty() ? null : classes.getFirst(), null, null);
    }

    private List<String> subjectCodes(Scope scope) {
        if (scope.empty()) {
            return List.of();
        }
        return jdbc.query("select distinct subject_code from course_sessions where " + scope.filter(),
                (rs, n) -> rs.getString(1), scope.filterParams());
    }

    private static LocalDate today() {
        return LocalDate.now(SCHOOL_ZONE);
    }

    private static List<Object> concat(List<String> subjects, Object last) {
        final List<Object> all = new ArrayList<>(subjects);
        all.add(last);
        return all;
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}