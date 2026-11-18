package org.linkwave.chatservice.bot;

import feign.FeignException;
import org.linkwave.chatservice.api.academic.AcademicClient;
import org.linkwave.chatservice.api.academic.AcademicClient.ClassView;
import org.linkwave.chatservice.api.academic.AcademicClient.DutyView;
import org.linkwave.chatservice.api.academic.AcademicClient.ExamView;
import org.linkwave.chatservice.api.academic.AcademicClient.SessionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Chuyển dữ liệu học vụ thành tin nhắn cho các lệnh /tkb, /lichthi, /lop, /lichgac. */
@Component
public class AcademicBotHandler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int TIMETABLE_DAYS = 7;
    private static final DateTimeFormatter DAY_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String UNAVAILABLE = "Dữ liệu học vụ tạm thời chưa truy cập được. Vui lòng thử lại sau.";

    private final AcademicClient client;
    private final String serviceKey;

    public AcademicBotHandler(AcademicClient client, @Value("${internal.service-key}") String serviceKey) {
        this.client = client;
        this.serviceKey = serviceKey;
    }

    /** Thời khóa biểu 7 ngày kể từ hôm nay */
    public String timetable(Long userId) {
        final LocalDate today = LocalDate.now(ZONE);
        final LocalDate last = today.plusDays(TIMETABLE_DAYS - 1);
        try {
            final var sessions = client.timetable(serviceKey, userId, today, last);
            final var terms = client.terms(serviceKey, today);

            final StringBuilder sb = new StringBuilder("Thời khóa biểu từ ")
                    .append(today.format(DAY_FULL)).append(" đến ").append(last.format(DAY_FULL));
            if (!terms.isEmpty()) {
                sb.append("\nHọc kỳ: ").append(terms.getFirst().name());
            }
            if (sessions.isEmpty()) {
                return sb.append("\n\nKhông có tiết học nào trong 7 ngày tới.").toString();
            }
            LocalDate currentDay = null;
            for (SessionView s : sessions) {
                if (!s.date().equals(currentDay)) {
                    currentDay = s.date();
                    sb.append("\n\n").append(dayLabel(s.date())).append(" (").append(s.date().format(DAY_FULL)).append(")");
                }
                sb.append("\n- ").append(s.startTime().format(TIME))
                        .append(" · Tiết ").append(periods(s.startPeriod(), s.periodCount()))
                        .append(" · ").append(name(s.subjectName(), s.subjectCode()));
                if (s.groupCode() != null && !s.groupCode().isBlank()) {
                    sb.append(" · Nhóm ").append(s.groupCode());
                }
                if (s.room() != null) {
                    sb.append(" · Phòng ").append(s.room());
                }
                sb.append(" · Lớp ").append(s.classCode());
            }
            return sb.toString();
        } catch (FeignException e) {
            return UNAVAILABLE;
        }
    }

    /** Lịch thi sắp tới của các môn trong thời khóa biểu */
    public String exams(Long userId) {
        try {
            final var exams = client.exams(serviceKey, userId);
            if (exams.isEmpty()) {
                return "Chưa có lịch thi nào sắp tới cho các môn của bạn.";
            }
            final StringBuilder sb = new StringBuilder("Lịch thi sắp tới:");
            for (ExamView e : exams) {
                sb.append("\n- ").append(e.date().format(DAY_FULL)).append(" ").append(e.time().format(TIME))
                        .append(" · ").append(name(e.subjectName(), e.subjectCode()))
                        .append(" · Phòng ").append(e.room());
                if (e.campus() != null) {
                    sb.append(" · Cơ sở ").append(e.campus());
                }
            }
            return sb.toString();
        } catch (FeignException e) {
            return UNAVAILABLE;
        }
    }

    /** Sinh viên: lớp và số sinh viên cùng lớp. Giảng viên: các lớp đang dạy. */
    public String classes(Long userId) {
        try {
            final var classes = client.classes(serviceKey, userId);
            if (classes.isEmpty()) {
                return "Bạn chưa được gán lớp nào trong dữ liệu học vụ.";
            }
            if (classes.size() == 1 && classes.getFirst().classmates() != null) {
                final ClassView own = classes.getFirst();
                return "Lớp của bạn: " + own.classCode() + " (" + own.classmates() + " sinh viên)";
            }
            final StringBuilder sb = new StringBuilder("Các lớp bạn đang dạy:");
            for (ClassView c : classes) {
                sb.append("\n- ").append(c.classCode());
            }
            return sb.toString();
        } catch (FeignException e) {
            return UNAVAILABLE;
        }
    }

    /** Lịch gác thi sắp tới của giảng viên */
    public String duties(Long userId) {
        try {
            final var duties = client.duties(serviceKey, userId);
            if (duties.isEmpty()) {
                return "Bạn chưa có lịch gác thi nào sắp tới.";
            }
            final StringBuilder sb = new StringBuilder("Lịch gác thi sắp tới:");
            for (DutyView d : duties) {
                sb.append("\n- ").append(d.date().format(DAY_FULL)).append(" ").append(d.time().format(TIME))
                        .append(" · Phòng ").append(d.room());
                if (d.subjectCode() != null) {
                    sb.append(" · ").append(name(d.subjectName(), d.subjectCode()));
                }
                if (d.campus() != null) {
                    sb.append(" · Cơ sở ").append(d.campus());
                }
            }
            return sb.toString();
        } catch (FeignException e) {
            return UNAVAILABLE;
        }
    }

    private static String periods(int start, int count) {
        return count <= 1 ? String.valueOf(start) : start + "–" + (start + count - 1);
    }

    /** Tên môn kèm mã, ví dụ "Lập trình Java (IT101)". Thiếu tên thì chỉ hiện mã. */
    private static String name(String subjectName, String subjectCode) {
        return subjectName == null || subjectName.isBlank()
                ? subjectCode
                : subjectName + " (" + subjectCode + ")";
    }

    private static String dayLabel(LocalDate date) {
        final DayOfWeek day = date.getDayOfWeek();
        return switch (day) {
            case SUNDAY -> "Chủ nhật";
            default -> "Thứ " + (day.getValue() + 1);
        };
    }
}