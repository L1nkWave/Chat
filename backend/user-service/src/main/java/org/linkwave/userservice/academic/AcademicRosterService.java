package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Danh sách thành viên của các nhóm học vụ (lớp, môn, khoa) cho chat-service đồng bộ.
 * Chỉ tính người dùng chưa bị xóa. Giảng viên được khớp theo tên đăng nhập (mã giảng viên), hoặc theo họ tên
 * khi thiếu mã; họ tên trùng nhiều giảng viên thì đưa vào danh sách ambiguous và không thêm vào nhóm.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicRosterService {

    private final JdbcTemplate jdbc;

    public record ClassRoster(String classCode, String className, String facultyCode, String facultyName,
                              Long advisorUserId, List<Long> studentUserIds) {
    }

    public record SubjectRoster(String subjectCode, String subjectName, List<Long> studentUserIds,
                                List<Long> lecturerUserIds) {
    }

    public record FacultyRoster(String facultyCode, String facultyName, List<Long> userIds) {
    }

    public record AmbiguousLecturer(String subjectCode, String lecturerName, List<Long> candidateUserIds) {
    }

    public record Rosters(List<ClassRoster> classes,
                          List<SubjectRoster> subjects,
                          List<FacultyRoster> faculties,
                          List<AmbiguousLecturer> ambiguous,
                          List<String> unmatchedLecturers) {
    }

    private record ClassRow(String code, String name, String facultyCode, String facultyName, Long advisorId) {
    }

    private record SessionRow(String subjectCode, String classCode, String lecturerCode, String lecturerName) {
    }

    public Rosters rosters() {
        final List<ClassRow> classRows = jdbc.query("""
                        select c.code, c.name, f.code as faculty_code, f.name as faculty_name, a.id as advisor_id
                        from school_classes c
                        join faculties f on f.id = c.faculty_id
                        left join users a on a.id = c.advisor_id and a.is_deleted = false
                        order by c.code
                        """,
                (rs, n) -> new ClassRow(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("faculty_code"),
                        rs.getString("faculty_name"),
                        rs.getObject("advisor_id", Long.class)));

        // sinh viên theo lớp (chỉ người chưa bị xóa)
        final Map<String, List<Long>> studentsByClass = new LinkedHashMap<>();
        jdbc.query("""
                        select sp.class_code, sp.user_id
                        from student_profiles sp
                        join users u on u.id = sp.user_id
                        where sp.class_code is not null and u.is_deleted = false
                        order by sp.class_code, sp.user_id
                        """,
                rs -> {
                    studentsByClass.computeIfAbsent(rs.getString("class_code"), k -> new ArrayList<>())
                            .add(rs.getLong("user_id"));
                });

        final List<ClassRoster> classes = new ArrayList<>();
        final Map<String, Set<Long>> usersByFaculty = new TreeMap<>();
        for (ClassRow row : classRows) {
            final List<Long> students = studentsByClass.getOrDefault(row.code(), List.of());
            classes.add(new ClassRoster(row.code(), row.name(), row.facultyCode(), row.facultyName(),
                    row.advisorId(), students));

            final Set<Long> facultyUsers = usersByFaculty.computeIfAbsent(row.facultyCode(), k -> new LinkedHashSet<>());
            facultyUsers.addAll(students);
            if (row.advisorId() != null) {
                facultyUsers.add(row.advisorId());
            }
        }

        final List<FacultyRoster> faculties = new ArrayList<>();
        jdbc.query("select code, name from faculties order by code", rs -> {
            final String code = rs.getString("code");
            final Set<Long> users = usersByFaculty.getOrDefault(code, Set.of());
            faculties.add(new FacultyRoster(code, rs.getString("name"), new ArrayList<>(users)));
        });

        return buildSubjectsAndLecturers(classes, studentsByClass, faculties);
    }

    private Rosters buildSubjectsAndLecturers(List<ClassRoster> classes,
                                              Map<String, List<Long>> studentsByClass,
                                              List<FacultyRoster> faculties) {
        // tên môn: ưu tiên danh mục môn học, sau đó tên ghi trên thời khóa biểu
        final Map<String, String> subjectNames = new TreeMap<>();
        jdbc.query("select subject_code, max(subject_name) as subject_name from course_sessions "
                        + "where subject_name is not null group by subject_code",
                rs -> {
                    subjectNames.put(rs.getString("subject_code"), rs.getString("subject_name"));
                });
        jdbc.query("select code, name from subjects", rs -> {
            subjectNames.put(rs.getString("code"), rs.getString("name"));
        });

        final List<SessionRow> sessions = jdbc.query("""
                        select distinct subject_code, class_code, lecturer_code, lecturer_name
                        from course_sessions
                        order by subject_code
                        """,
                (rs, n) -> new SessionRow(
                        rs.getString("subject_code"),
                        rs.getString("class_code"),
                        rs.getString("lecturer_code"),
                        rs.getString("lecturer_name")));

        final Lecturers lecturers = loadLecturers();

        final Map<String, Set<Long>> studentsBySubject = new TreeMap<>();
        final Map<String, Set<Long>> lecturersBySubject = new TreeMap<>();
        final List<AmbiguousLecturer> ambiguous = new ArrayList<>();
        final Set<String> unmatched = new LinkedHashSet<>();

        for (SessionRow row : sessions) {
            studentsBySubject.computeIfAbsent(row.subjectCode(), k -> new LinkedHashSet<>())
                    .addAll(studentsByClass.getOrDefault(row.classCode(), List.of()));

            final Set<Long> subjectLecturers = lecturersBySubject.computeIfAbsent(row.subjectCode(), k -> new LinkedHashSet<>());
            final Long byCode = row.lecturerCode() == null ? null : lecturers.byUsername().get(row.lecturerCode().strip());
            if (byCode != null) {
                subjectLecturers.add(byCode);
                continue;
            }
            final String name = row.lecturerName();
            if (name == null || name.isBlank()) {
                if (row.lecturerCode() != null && !row.lecturerCode().isBlank()) {
                    unmatched.add(row.lecturerCode().strip());
                }
                continue;
            }
            final List<Long> candidates = lecturers.byName().getOrDefault(normalizeName(name), List.of());
            if (candidates.size() == 1) {
                subjectLecturers.add(candidates.getFirst());
            } else if (candidates.size() > 1) {
                ambiguous.add(new AmbiguousLecturer(row.subjectCode(), name.strip(), List.copyOf(candidates)));
            } else {
                unmatched.add(name.strip());
            }
        }

        final Set<String> subjectCodes = new LinkedHashSet<>(studentsBySubject.keySet());
        subjectCodes.addAll(subjectNames.keySet());
        final List<SubjectRoster> subjects = new ArrayList<>();
        for (String code : subjectCodes) {
            subjects.add(new SubjectRoster(
                    code,
                    subjectNames.getOrDefault(code, code),
                    new ArrayList<>(studentsBySubject.getOrDefault(code, Set.of())),
                    new ArrayList<>(lecturersBySubject.getOrDefault(code, Set.of()))));
        }

        return new Rosters(
                classes,
                subjects,
                faculties,
                ambiguous.stream().distinct().toList(),
                List.copyOf(unmatched));
    }

    /** Giảng viên theo tên đăng nhập và theo họ tên đã chuẩn hóa */
    private record Lecturers(Map<String, Long> byUsername, Map<String, List<Long>> byName) {
    }

    private Lecturers loadLecturers() {
        final Map<String, Long> byUsername = new LinkedHashMap<>();
        final Map<String, List<Long>> byName = new LinkedHashMap<>();
        jdbc.query("""
                        select distinct u.id, u.username, u.name
                        from users u
                        join user_roles ur on ur.users_id = u.id
                        join roles r on r.id = ur.roles_id
                        where r.name = 'ROLE_LECTURER' and u.is_deleted = false
                        order by u.id
                        """,
                rs -> {
                    final long id = rs.getLong("id");
                    final String username = rs.getString("username");
                    if (username != null) {
                        byUsername.put(username.strip(), id);
                    }
                    byName.computeIfAbsent(normalizeName(rs.getString("name")), k -> new ArrayList<>()).add(id);
                });
        return new Lecturers(byUsername, byName);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
