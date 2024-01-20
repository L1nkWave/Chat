package org.linkwave.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private List<User> users = new LinkedList<>();

    @Getter
    @RequiredArgsConstructor
    public enum Roles {

        USER("ROLE_USER"),
        ADMIN("ROLE_ADMIN");

        private final String name;

    }

}