package org.linkwave.auth.repository;

import org.linkwave.auth.entity.DeactivatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface DeactivatedTokenRepository extends JpaRepository<DeactivatedToken, UUID> {

    @Modifying
    @Query(
            value = "delete from deactivated_tokens where expiration < :timeAgo",
            nativeQuery = true
    )
    void removeAllExpiredTokens(Instant timeAgo);

}
