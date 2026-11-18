package org.linkwave.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString(exclude = {"users", "permissions"})
@EqualsAndHashCode(exclude = {"users", "permissions"})
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    /** Tên tiếng Việt hiển thị cho người dùng quản trị. Có thể để trống; khi trống dùng nhãn mặc định. */
    @Column(name = "display_name", length = 128)
    private String displayName;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private List<UserEntity> users = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<PermissionEntity> permissions = new HashSet<>();

    @Getter
    @RequiredArgsConstructor
    public enum Roles {

        ROOT("ROLE_ROOT", 4),
        ADMIN("ROLE_ADMIN", 3),
        LECTURER("ROLE_LECTURER", 2),
        STUDENT("ROLE_STUDENT", 1),
        USER("ROLE_USER", 0);

        private final String value;
        private final int level;

        /** Cấp của một vai trò toàn cục; vai trò không xác định có cấp 0. */
        public static int levelOf(String roleName) {
            for (Roles role : values()) {
                if (role.value.equals(roleName)) {
                    return role.level;
                }
            }
            return 0;
        }

    }

}