package org.linkwave.chatservice.api.academic;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Gọi API nội bộ của user-service để lấy dữ liệu học vụ cho bot. Khóa nội bộ gửi qua header X-Internal-Key. */
// contextId phải khác với client khác cùng trỏ tới user-service, nếu không Spring báo trùng bean
@FeignClient(value = "user-service", contextId = "academicClient", path = "/internal/academic")
public interface AcademicClient {

    @GetMapping("/timetable")
    List<SessionView> timetable(@RequestHeader("X-Internal-Key") String key,
                                @RequestParam("userId") Long userId,
                                @RequestParam("from") LocalDate from,
                                @RequestParam("to") LocalDate to);

    @GetMapping("/exams")
    List<ExamView> exams(@RequestHeader("X-Internal-Key") String key, @RequestParam("userId") Long userId);

    @GetMapping("/duties")
    List<DutyView> duties(@RequestHeader("X-Internal-Key") String key, @RequestParam("userId") Long userId);

    @GetMapping("/classes")
    List<ClassView> classes(@RequestHeader("X-Internal-Key") String key, @RequestParam("userId") Long userId);

    @GetMapping("/terms")
    List<TermView> terms(@RequestHeader("X-Internal-Key") String key, @RequestParam("date") LocalDate date);

    @GetMapping("/rosters")
    RostersView rosters(@RequestHeader("X-Internal-Key") String key);

    // Các record khớp với JSON của user-service (AcademicQueryService)
    record SessionView(LocalDate date, int startPeriod, int periodCount, LocalTime startTime,
                       String subjectCode, String subjectName, String groupCode, String room,
                       String classCode, String lecturerCode) {
    }

    record ExamView(LocalDate date, LocalTime time, String subjectCode, String subjectName,
                    String room, String campus) {
    }

    record DutyView(LocalDate date, LocalTime time, String subjectCode, String subjectName,
                    String room, String campus) {
    }

    record ClassView(String classCode, Integer classmates) {
    }

    record TermView(String code, String name, LocalDate startDate, LocalDate endDate) {
    }

    // Các record khớp với JSON của user-service (AcademicRosterService)
    record RostersView(List<ClassRoster> classes, List<SubjectRoster> subjects, List<FacultyRoster> faculties,
                       List<AmbiguousLecturer> ambiguous, List<String> unmatchedLecturers) {
    }

    record ClassRoster(String classCode, String className, String facultyCode, String facultyName,
                       Long advisorUserId, List<Long> studentUserIds) {
    }

    record SubjectRoster(String subjectCode, String subjectName, List<Long> studentUserIds,
                         List<Long> lecturerUserIds) {
    }

    record FacultyRoster(String facultyCode, String facultyName, List<Long> userIds) {
    }

    /** Giảng viên trùng họ tên, không xác định được là ai: không được tự động thêm vào nhóm */
    record AmbiguousLecturer(String subjectCode, String lecturerName, List<Long> candidateUserIds) {
    }
}