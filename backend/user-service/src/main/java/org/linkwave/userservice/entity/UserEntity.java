package org.linkwave.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.ZonedDateTime.now;

@Entity
@Table(name = "users")
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

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    private String avatarPath;

    @Column(nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = now();

    @Column(nullable = false)
    @Builder.Default
    private ZonedDateTime lastSeen = now().plusSeconds(1L);

    @Column(nullable = false)
    private boolean theme;

    @Column(nullable = false)
    private boolean isOnline;

    private String bio;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private List<RoleEntity> roles = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "user_id_1")
    @Builder.Default
    private List<ContactEntity> contacts = new ArrayList<>();

}