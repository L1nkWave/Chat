package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Thêm, sửa, xóa khoa và lớp bằng form. Dùng cùng bảng với import Excel nên không tạo bản ghi trùng.
 * Không xóa khoa còn lớp, không xóa lớp còn sinh viên hoặc thời khóa biểu, không đổi mã lớp đã có sinh viên.
 */
@Service
@RequiredArgsConstructor
public class AcademicCatalogService {

    private static final Pattern FACULTY_CODE = Pattern.compile("[A-Za-z0-9._-]{1,16}");
    private static final Pattern CLASS_CODE = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    public record FacultyView(long id, String code, String name, long classCount) {
    }

    public record ClassView(long id, String code, String name, long facultyId, String facultyCode,
                            String cohort, long studentCount) {
    }

    public record FacultyInput(String code, String name) {
    }

    public record ClassInput(String code, String name, Long facultyId, String cohort) {
    }

    private final JdbcTemplate jdbc;

    public List<FacultyView> faculties() {
        return jdbc.query("""
                select f.id, f.code, f.name,
                       (select count(*) from school_classes c where c.faculty_id = f.id) as class_count
                from faculties f
                order by f.code
                """,
                (rs, n) -> new FacultyView(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getLong("class_count")));
    }

    public List<ClassView> classes() {
        return jdbc.query("""
                select c.id, c.code, c.name, c.faculty_id, f.code as faculty_code, c.cohort,
                       (select count(*) from student_profiles p where p.class_code = c.code) as student_count
                from school_classes c
                join faculties f on f.id = c.faculty_id
                order by c.code
                """,
                (rs, n) -> new ClassView(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getLong("faculty_id"), rs.getString("faculty_code"), rs.getString("cohort"),
                        rs.getLong("student_count")));
    }

    @Transactional
    public void saveFaculty(Long id, FacultyInput input) {
        final String code = input.code().trim();
        final String name = input.name().trim();
        if (!FACULTY_CODE.matcher(code).matches()) {
            throw new UnacceptableRequestDataException("Mã khoa không hợp lệ (chữ, số, dấu . _ -, tối đa 16 ký tự)");
        }
        if (name.isEmpty() || name.length() > 128) {
            throw new UnacceptableRequestDataException("Tên khoa trống hoặc dài quá 128 ký tự");
        }
        try {
            if (id == null) {
                jdbc.update("insert into faculties (code, name, created_at) values (?, ?, now())", code, name);
            } else {
                final int updated = jdbc.update("update faculties set code = ?, name = ? where id = ?", code, name, id);
                if (updated == 0) {
                    throw new ResourceNotFoundException("Không tìm thấy khoa");
                }
            }
        } catch (DuplicateKeyException e) {
            throw new UnacceptableRequestDataException("Mã khoa " + code + " đã tồn tại");
        }
    }

    @Transactional
    public void deleteFaculty(long id) {
        final Long classes = jdbc.queryForObject("select count(*) from school_classes where faculty_id = ?", Long.class, id);
        if (classes != null && classes > 0) {
            throw new UnacceptableRequestDataException("Khoa còn " + classes + " lớp. Xóa hoặc chuyển các lớp trước");
        }
        if (jdbc.update("delete from faculties where id = ?", id) == 0) {
            throw new ResourceNotFoundException("Không tìm thấy khoa");
        }
    }

    @Transactional
    public void saveClass(Long id, ClassInput input) {
        final String code = input.code().trim();
        final String name = input.name().trim();
        final String cohort = input.cohort() == null || input.cohort().isBlank() ? null : input.cohort().trim();
        if (!CLASS_CODE.matcher(code).matches()) {
            throw new UnacceptableRequestDataException("Mã lớp không hợp lệ (ví dụ DCT1222)");
        }
        if (name.isEmpty() || name.length() > 128) {
            throw new UnacceptableRequestDataException("Tên lớp trống hoặc dài quá 128 ký tự");
        }
        if (cohort != null && cohort.length() > 8) {
            throw new UnacceptableRequestDataException("Niên khóa quá dài (tối đa 8 ký tự, ví dụ K23)");
        }
        final Long facultyExists = jdbc.queryForObject("select count(*) from faculties where id = ?", Long.class, input.facultyId());
        if (facultyExists == null || facultyExists == 0) {
            throw new UnacceptableRequestDataException("Khoa đã chọn không tồn tại");
        }

        try {
            if (id == null) {
                jdbc.update("insert into school_classes (code, name, faculty_id, cohort, created_at) values (?, ?, ?, ?, now())",
                        code, name, input.facultyId(), cohort);
                return;
            }
            final String oldCode = jdbc.query("select code from school_classes where id = ?",
                            (rs, n) -> rs.getString(1), id).stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp"));
            // Sinh viên lưu mã lớp dạng chuỗi, nên đổi mã lớp đã có sinh viên sẽ làm họ mất lớp
            if (!oldCode.equals(code) && studentCount(oldCode) > 0) {
                throw new UnacceptableRequestDataException("Không đổi mã lớp đã có sinh viên");
            }
            jdbc.update("update school_classes set code = ?, name = ?, faculty_id = ?, cohort = ? where id = ?",
                    code, name, input.facultyId(), cohort, id);
        } catch (DuplicateKeyException e) {
            throw new UnacceptableRequestDataException("Mã lớp " + code + " đã tồn tại");
        }
    }

    @Transactional
    public void deleteClass(long id) {
        final String code = jdbc.query("select code from school_classes where id = ?",
                        (rs, n) -> rs.getString(1), id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp"));
        final long students = studentCount(code);
        final Long sessions = jdbc.queryForObject("select count(*) from course_sessions where class_code = ?", Long.class, code);
        if (students > 0 || (sessions != null && sessions > 0)) {
            throw new UnacceptableRequestDataException("Lớp " + code + " còn sinh viên hoặc thời khóa biểu. Không xóa được");
        }
        jdbc.update("delete from school_classes where id = ?", id);
    }

    private long studentCount(String classCode) {
        final Long count = jdbc.queryForObject("select count(*) from student_profiles where class_code = ?", Long.class, classCode);
        return count == null ? 0 : count;
    }
}