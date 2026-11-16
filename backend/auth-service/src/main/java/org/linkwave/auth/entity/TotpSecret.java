package org.linkwave.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/** Khóa TOTP của một tài khoản. Khóa bí mật được mã hóa AES-256-GCM, không lưu dạng rõ. */
@Entity
@Table(name = "totp_secrets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "secretEncrypted")
public class TotpSecret {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "secret_encrypted", nullable = false, columnDefinition = "text")
    private String secretEncrypted;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = false;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private ZonedDateTime lockedUntil;

    /** Bước TOTP gần nhất đã dùng thành công, chặn dùng lại cùng một mã. */
    @Builder.Default
    @Column(name = "last_used_step", nullable = false)
    private long lastUsedStep = -1L;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = now();

    @Column(name = "enabled_at")
    private ZonedDateTime enabledAt;

}