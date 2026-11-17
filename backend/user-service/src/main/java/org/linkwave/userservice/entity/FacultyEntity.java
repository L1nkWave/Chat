package org.linkwave.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/** Khoa của trường. Mã khoa dùng cho phạm vi FACULTY khi cấp vai trò. */
@Entity
@Table(name = "faculties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}