package org.linkwave.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.ZonedDateTime;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1")
    private Long ownerId;

    @ManyToOne
    @JoinColumn(name = "user_id_2")
    private UserEntity user;

    @Builder.Default
    private ZonedDateTime addedAt = ZonedDateTime.now();

    private String alias;

}
