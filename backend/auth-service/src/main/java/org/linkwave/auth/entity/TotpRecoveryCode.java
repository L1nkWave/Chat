package org.linkwave.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/** Mã khôi phục dùng một lần. Chỉ lưu bản băm BCrypt. */
@Entity
@Table(name = "totp_recovery_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = now();

}