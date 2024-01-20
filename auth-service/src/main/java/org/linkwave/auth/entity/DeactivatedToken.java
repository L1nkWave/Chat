package org.linkwave.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deactivated_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeactivatedToken {

    @Id
    private UUID id;

    private Instant expiration;

}
