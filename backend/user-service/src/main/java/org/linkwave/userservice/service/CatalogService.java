package org.linkwave.userservice.service;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.entity.FacultyEntity;
import org.linkwave.userservice.entity.ScopeType;
import org.linkwave.userservice.entity.SchoolClassEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.repository.FacultyRepository;
import org.linkwave.userservice.repository.SchoolClassRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final int SEARCH_LIMIT = 50;

    private final FacultyRepository facultyRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FacultyEntity> searchFaculties(String q) {
        return facultyRepository.search(pattern(q), PageRequest.of(0, SEARCH_LIMIT));
    }

    @Transactional(readOnly = true)
    public List<SchoolClassEntity> searchClasses(String q, Long facultyId) {
        return classRepository.search(pattern(q), facultyId == null ? 0L : facultyId, PageRequest.of(0, SEARCH_LIMIT));
    }

    @Transactional
    public FacultyEntity createFaculty(String code, String name) {
        final String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (facultyRepository.existsByCode(normalized)) {
            throw new UnacceptableRequestDataException("Mã khoa đã tồn tại");
        }
        return facultyRepository.save(FacultyEntity.builder()
                .code(normalized)
                .name(name.trim())
                .build());
    }

    @Transactional
    public SchoolClassEntity createClass(String code, String name, Long facultyId, String cohort, Long advisorId) {
        final String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (classRepository.existsByCode(normalized)) {
            throw new UnacceptableRequestDataException("Mã lớp đã tồn tại");
        }
        final FacultyEntity faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoa"));
        if (advisorId != null && !userRepository.existsById(advisorId)) {
            throw new ResourceNotFoundException("Không tìm thấy cố vấn học tập");
        }
        final String cohortValue = cohort == null || cohort.isBlank() ? null : cohort.trim().toUpperCase(Locale.ROOT);
        return classRepository.save(SchoolClassEntity.builder()
                .code(normalized)
                .name(name.trim())
                .faculty(faculty)
                .cohort(cohortValue)
                .advisorId(advisorId)
                .build());
    }

    /** Khi cấp vai trò theo phạm vi: khoa hoặc lớp phải tồn tại. SCHOOL không cần mã. */
    @Transactional(readOnly = true)
    public void requireScopeExists(ScopeType scopeType, Long scopeId) {
        if (scopeType == ScopeType.SCHOOL) {
            return;
        }
        if (scopeId == null) {
            throw new UnacceptableRequestDataException("Phạm vi khoa hoặc lớp cần có mã");
        }
        if (scopeType == ScopeType.FACULTY) {
            if (!facultyRepository.existsById(scopeId)) {
                throw new ResourceNotFoundException("Không tìm thấy khoa");
            }
        } else if (!classRepository.existsById(scopeId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp");
        }
    }

    private static String pattern(String q) {
        return "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
    }
}