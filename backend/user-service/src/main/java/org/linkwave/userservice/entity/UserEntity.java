package org.linkwave.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.ZonedDateTime.now;

@Entity
@Table(
        name = "users",
        indexes = @Index(columnList = "username", unique = true)
)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(of = {"id", "username", "name"})
@JsonIgnoreProperties({"password", "roles", "contacts"})
@ToString(exclude = {"password", "roles", "contacts"})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    private String avatarPath;

    @ColumnDefault("false")
    private boolean isDeleted;

    @ColumnDefault("false")
    private boolean isBlocked;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE default now()")
    @Builder.Default
    private ZonedDateTime createdAt = now();

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE default now()")
    @Builder.Default
    private ZonedDateTime lastSeen = now().plusSeconds(1L);

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean theme;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean isOnline;

    private String bio;

    /** Cột dùng chung với auth-service: refresh token cấp trước mốc này bị từ chối (đăng xuất, đổi mật khẩu, đổi vai trò) */
    @JsonIgnore
    private Instant tokensValidAfter;

    /** Cột dùng chung với auth-service: phải đổi mật khẩu ở lần đăng nhập tiếp theo (sau khi admin reset) */
    @JsonIgnore
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean mustChangePassword;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            uniqueConstraints = @UniqueConstraint(
                    name = "UC_rd_uid",
                    columnNames = {"roles_id", "users_id"}
            )
    )
    @Builder.Default
    private List<RoleEntity> roles = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "user_id_1")
    @Builder.Default
    private List<ContactEntity> contacts = new ArrayList<>();

}