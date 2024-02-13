package org.linkwave.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString(exclude = "users")
@EqualsAndHashCode(exclude = "users")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private List<UserEntity> users = new ArrayList<>();

    @Getter
    @RequiredArgsConstructor
    public enum Roles {

        USER("ROLE_USER"),
        ADMIN("ROLE_ADMIN");

        private final String value;

    }

}