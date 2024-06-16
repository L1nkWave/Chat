package org.linkwave.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static java.time.ZonedDateTime.now;

@Entity
@Table(
        name = "users",
        indexes = @Index(columnList = "username", unique = true)
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String username;

    private String password;

    @ColumnDefault("false")
    private boolean isDeleted;

    @ColumnDefault("false")
    private boolean isBlocked;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean isOnline;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE default now()")
    @Builder.Default
    private ZonedDateTime lastSeen = now().plusSeconds(1L);

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            uniqueConstraints = @UniqueConstraint(
                    name = "UC_rd_uid",
                    columnNames = {"roles_id", "users_id"}
            )
    )
    @Builder.Default
    private List<Role> roles = new LinkedList<>();

    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

}
