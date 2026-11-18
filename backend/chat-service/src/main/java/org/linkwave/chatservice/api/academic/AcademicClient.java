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
}