package org.linkwave.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/** Ghi vào cùng bảng audit_logs với user-service. */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(length = 64)
    private String ip;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = now();

}