package org.linkwave.userservice.academic;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.academic.AcademicImportService.TermInput;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Tìm kiếm, sửa, xóa bản ghi trong các tab phụ của Học vụ: môn học, thời khóa biểu, lịch thi, lịch gác thi, học kỳ.
 * Tên bảng và cột lấy từ danh sách cố định bên dưới, không lấy từ đầu vào của người dùng.
 * Giá trị luôn đi qua tham số ?, không ghép chuỗi.
 */
@Service
@RequiredArgsConstructor
public class AcademicRecordService {

    public enum Kind { SUBJECTS, TIMETABLE, EXAMS, DUTIES, TERMS }

    public enum ColType { TEXT, INT, DATE, TIME }

    /** searchable: tham gia tìm kiếm. editable: sửa được. nullable: cho phép để trống. */
    public record Col(String name, ColType type, int maxLen, boolean searchable, boolean editable, boolean nullable) {
    }

    private record Spec(String table, String keyColumn, boolean numericKey, String orderBy, List<Col> cols) {
    }

    public record RecordPage(List<Map<String, Object>> items, long total, int page, int size) {
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9._-]{1,32}");
    private static final Pattern SUBJECT_CODE = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    private final JdbcTemplate jdbc;
    private final AcademicImportService importService;

    private static Col text(String name, int max, boolean searchable, boolean editable, boolean nullable) {
        return new Col(name, ColType.TEXT, max, searchable, editable, nullable);
    }

    private static Col plain(String name, ColType type) {
        return new Col(name, type, 0, false, false, true);
    }

    private static Col editable(String name, ColType type, boolean nullable) {
        return new Col(name, type, 255, false, true, nullable);
    }

    private static Spec spec(Kind kind) {
        return switch (kind) {
            case SUBJECTS -> new Spec("subjects", "code", false, "code", List.of(
                    text("code", 32, true, false, false),
                    text("name", 255, true, true, false)));
            case TIMETABLE -> new Spec("course_sessions", "id", true, "session_date, start_period", List.of(
                    text("class_code", 32, true, false, false),
                    text("subject_code", 32, true, false, false),
                    text("subject_name", 255, true, true, true),
                    text("group_code", 32, false, false, true),
                    text("room", 32, true, true, true),
                    plain("weekday", ColType.INT),
                    plain("start_period", ColType.INT),
                    plain("period_count", ColType.INT),
                    text("lecturer_code", 32, true, true, true),
                    text("lecturer_name", 255, true, true, true),
                    new Col("session_date", ColType.DATE, 0, false, true, false),
                    new Col("start_time", ColType.TIME, 0, false, true, false)));
            case EXAMS -> new Spec("exam_schedules", "id", true, "exam_date, exam_time", List.of(
                    text("subject_code", 32, true, false, false),
                    text("subject_name", 255, true, true, true),
                    new Col("candidate_count", ColType.INT, 0, false, true, true),
                    new Col("exam_date", ColType.DATE, 0, false, true, false),
                    new Col("exam_time", ColType.TIME, 0, false, true, false),
                    text("room", 32, true, true, false),
                    text("campus", 8, false, true, true)));
            case DUTIES -> new Spec("exam_duties", "id", true, "exam_date, exam_time", List.of(
                    text("lecturer_code", 32, true, true, false),
                    text("subject_code", 32, true, true, true),
                    new Col("exam_date", ColType.DATE, 0, false, true, false),
                    new Col("exam_time", ColType.TIME, 0, false, true, false),
                    text("room", 32, true, true, false),
                    text("campus", 8, false, true, true)));
            case TERMS -> new Spec("academic_terms", "code", false, "start_date desc", List.of(
                    text("code", 32, true, false, false),
                    text("name", 255, true, true, false),
                    new Col("start_date", ColType.DATE, 0, false, true, false),
                    new Col("end_date", ColType.DATE, 0, false, true, false)));
        };
    }

    public static Kind parseKind(String value) {
        try {
            return Kind.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnacceptableRequestDataException("Mục không hợp lệ. Dùng: subjects, timetable, exams, duties, terms");
        }
    }

    /** Danh sách có tìm kiếm chung theo q (không phân biệt hoa thường) và phân trang */
    @Transactional(readOnly = true)
    public RecordPage list(Kind kind, String q, int page, int size) {
        final Spec spec = spec(kind);
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        final List<Object> args = new ArrayList<>();

        final String where = whereClause(spec, q, args);
        final String select = spec.keyColumn() + " as key, "
                + String.join(", ", spec.cols().stream().map(AcademicRecordService::selectExpr).toList());

        final Long total = jdbc.queryForObject("select count(*) from " + spec.table() + " where " + where, Long.class, args.toArray());
        final List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(safePage * safeSize);
        final List<Map<String, Object>> items = jdbc.queryForList(
                "select " + select + " from " + spec.table() + " where " + where
                        + " order by " + spec.orderBy() + " limit ? offset ?",
                pageArgs.toArray());
        return new RecordPage(items, total == null ? 0 : total, safePage, safeSize);
    }

    /** Sửa một bản ghi. Với học kỳ, dùng lại kiểm tra ngày trùng của học kỳ. */
    @Transactional
    public void update(Kind kind, String rawKey, Map<String, String> body) {
        if (kind == Kind.TERMS) {
            updateTerm(rawKey, body);
            return;
        }
        final Spec spec = spec(kind);
        final Object key = keyValue(spec, rawKey);
        final List<String> sets = new ArrayList<>();
        final List<Object> args = new ArrayList<>();

        for (Col col : spec.cols()) {
            if (!col.editable() || !body.containsKey(col.name())) {
                continue;
            }
            sets.add(col.name() + " = ?");
            args.add(parseValue(col, body.get(col.name())));
        }
        if (sets.isEmpty()) {
            throw new UnacceptableRequestDataException("Không có trường nào để sửa");
        }
        args.add(key);
        try {
            final int updated = jdbc.update("update " + spec.table() + " set " + String.join(", ", sets)
                    + " where " + spec.keyColumn() + " = ?", args.toArray());
            if (updated == 0) {
                throw new ResourceNotFoundException("Không tìm thấy bản ghi");
            }
        } catch (DuplicateKeyException e) {
            throw new UnacceptableRequestDataException("Dữ liệu trùng với một bản ghi đã có");
        }
    }

    /** Xóa một bản ghi. Môn học còn được dùng trong TKB hoặc lịch thi thì không xóa. */
    @Transactional
    public void delete(Kind kind, String rawKey) {
        final Spec spec = spec(kind);
        final Object key = keyValue(spec, rawKey);
        if (kind == Kind.SUBJECTS) {
            final Long used = jdbc.queryForObject(
                    "select (select count(*) from course_sessions where subject_code = ?)"
                            + " + (select count(*) from exam_schedules where subject_code = ?)", Long.class, key, key);
            if (used != null && used > 0) {
                throw new UnacceptableRequestDataException("Môn " + rawKey + " còn được dùng trong thời khóa biểu hoặc lịch thi");
            }
        }
        if (jdbc.update("delete from " + spec.table() + " where " + spec.keyColumn() + " = ?", key) == 0) {
            throw new ResourceNotFoundException("Không tìm thấy bản ghi");
        }
    }

    /** Thêm môn học bằng form. Các loại khác nhập bằng file Excel hoặc form riêng. */
    @Transactional
    public void createSubject(String code, String name) {
        final String cleanCode = code == null ? "" : code.trim();
        final String cleanName = name == null ? "" : name.trim();
        if (!SUBJECT_CODE.matcher(cleanCode).matches()) {
            throw new UnacceptableRequestDataException("Mã MH không hợp lệ (chữ, số, dấu . _ -, tối đa 32 ký tự)");
        }
        if (cleanName.isEmpty() || cleanName.length() > 255) {
            throw new UnacceptableRequestDataException("Tên môn học trống hoặc quá 255 ký tự");
        }
        try {
            jdbc.update("insert into subjects (code, name, created_at) values (?, ?, now())", cleanCode, cleanName);
        } catch (DuplicateKeyException e) {
            throw new UnacceptableRequestDataException("Mã MH " + cleanCode + " đã tồn tại");
        }
    }

    private void updateTerm(String code, Map<String, String> body) {
        final Long exists = jdbc.queryForObject("select count(*) from academic_terms where code = ?", Long.class, code);
        if (exists == null || exists == 0) {
            throw new ResourceNotFoundException("Không tìm thấy học kỳ");
        }
        final String name = body.getOrDefault("name", "");
        final LocalDate start = parseDateOrNull(body.get("start_date"));
        final LocalDate end = parseDateOrNull(body.get("end_date"));
        // saveTerm kiểm tra định dạng, khoảng ngày và trùng với học kỳ khác
        importService.saveTerm(new TermInput(code, name.trim(), start, end));
    }

    private static String whereClause(Spec spec, String q, List<Object> args) {
        final String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return "1 = 1";
        }
        final List<String> parts = new ArrayList<>();
        for (Col col : spec.cols()) {
            if (col.searchable()) {
                parts.add("lower(cast(" + col.name() + " as varchar)) like ?");
                args.add("%" + needle + "%");
            }
        }
        return parts.isEmpty() ? "1 = 1" : "(" + String.join(" or ", parts) + ")";
    }

    private static String selectExpr(Col col) {
        return switch (col.type()) {
            case DATE -> "to_char(" + col.name() + ", 'YYYY-MM-DD') as " + col.name();
            case TIME -> "to_char(" + col.name() + ", 'HH24:MI') as " + col.name();
            default -> col.name();
        };
    }

    private static Object keyValue(Spec spec, String raw) {
        if (spec.numericKey()) {
            try {
                return Long.valueOf(raw);
            } catch (NumberFormatException e) {
                throw new UnacceptableRequestDataException("Mã bản ghi không hợp lệ");
            }
        }
        return raw;
    }

    private static Object parseValue(Col col, String raw) {
        final String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            if (!col.nullable()) {
                throw new UnacceptableRequestDataException("Trường " + col.name() + " không được để trống");
            }
            return null;
        }
        return switch (col.type()) {
            case TEXT -> {
                if (value.length() > col.maxLen()) {
                    throw new UnacceptableRequestDataException("Trường " + col.name() + " quá " + col.maxLen() + " ký tự");
                }
                yield value;
            }
            case INT -> {
                try {
                    final int number = Integer.parseInt(value);
                    if (number < 0) {
                        throw new UnacceptableRequestDataException("Trường " + col.name() + " phải không âm");
                    }
                    yield number;
                } catch (NumberFormatException e) {
                    throw new UnacceptableRequestDataException("Trường " + col.name() + " phải là số");
                }
            }
            case DATE -> {
                final LocalDate date = parseDateOrNull(value);
                if (date == null) {
                    throw new UnacceptableRequestDataException("Trường " + col.name() + " phải dạng yyyy-MM-dd");
                }
                yield date;
            }
            case TIME -> {
                try {
                    yield LocalTime.parse(value);
                } catch (DateTimeParseException e) {
                    throw new UnacceptableRequestDataException("Trường " + col.name() + " phải dạng HH:mm");
                }
            }
        };
    }

    private static LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}