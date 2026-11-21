package org.linkwave.userservice.controller;

import org.linkwave.userservice.academic.AcademicQueryService;
import org.linkwave.userservice.academic.AcademicRosterService;
import org.linkwave.userservice.academic.AcademicRosterService.Rosters;
import org.linkwave.userservice.academic.AcademicQueryService.ClassView;
import org.linkwave.userservice.academic.AcademicQueryService.DutyView;
import org.linkwave.userservice.academic.AcademicQueryService.ExamView;
import org.linkwave.userservice.academic.AcademicQueryService.SessionView;
import org.linkwave.userservice.academic.AcademicQueryService.TermView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * API nội bộ cho chat-service (bot). Không đi qua gateway; mỗi lần gọi phải có header X-Internal-Key đúng.
 */
@RestController
@RequestMapping("/internal/academic")
public class InternalAcademicController {

    private static final String KEY_HEADER = "X-Internal-Key";

    private final AcademicQueryService query;
    private final AcademicRosterService rosterService;
    private final byte[] serviceKey;

    public InternalAcademicController(AcademicQueryService query,
                                      AcademicRosterService rosterService,
                                      @Value("${internal.service-key}") String serviceKey) {
        this.query = query;
        this.rosterService = rosterService;
        this.serviceKey = serviceKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/timetable")
    public List<SessionView> timetable(@RequestHeader(KEY_HEADER) String key,
                                       @RequestParam Long userId,
                                       @RequestParam LocalDate from,
                                       @RequestParam LocalDate to) {
        requireKey(key);
        return query.timetable(userId, from, to);
    }

    @GetMapping("/exams")
    public List<ExamView> exams(@RequestHeader(KEY_HEADER) String key, @RequestParam Long userId) {
        requireKey(key);
        return query.exams(userId);
    }

    @GetMapping("/duties")
    public List<DutyView> duties(@RequestHeader(KEY_HEADER) String key, @RequestParam Long userId) {
        requireKey(key);
        return query.duties(userId);
    }

    @GetMapping("/classes")
    public List<ClassView> classes(@RequestHeader(KEY_HEADER) String key, @RequestParam Long userId) {
        requireKey(key);
        return query.classes(userId);
    }

    @GetMapping("/terms")
    public List<TermView> terms(@RequestHeader(KEY_HEADER) String key, @RequestParam LocalDate date) {
        requireKey(key);
        return query.termsAt(date);
    }

    /** Thành viên của nhóm lớp, môn, khoa để chat-service đồng bộ. Không nhận tham số. */
    @GetMapping("/rosters")
    public Rosters rosters(@RequestHeader(KEY_HEADER) String key) {
        requireKey(key);
        return rosterService.rosters();
    }

    private void requireKey(String key) {
        if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), serviceKey)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Sai khóa dịch vụ nội bộ");
        }
    }
}