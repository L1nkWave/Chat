package org.linkwave.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/** Lớp sinh hoạt thuộc một khoa. cohort là niên khóa (ví dụ K23). advisorId là cố vấn học tập (mã người dùng). */
@Entity
@Table(name = "school_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @Column(length = 8)
    private String cohort;

    @Column(name = "advisor_id")
    private Long advisorId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}