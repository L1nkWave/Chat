package org.linkwave.auth.repository;

import org.linkwave.auth.entity.DeactivatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
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

    /**
     * Ghi token đã vô hiệu hóa, bỏ qua nếu đã có. Trả về 1 nếu ghi mới, 0 nếu token đã bị vô hiệu hóa trước đó.
     * Dùng một câu lệnh atomic nên an toàn khi hai yêu cầu chạy đồng thời.
     */
    @Transactional
    @Modifying
    @Query(
            value = "insert into deactivated_tokens (id, expiration) values (:id, :expiration) on conflict (id) do nothing",
            nativeQuery = true
    )
    int insertIfAbsent(@Param("id") UUID id, @Param("expiration") Timestamp expiration);

    default int insertIfAbsent(UUID id, Instant expiration) {
        return insertIfAbsent(id, Timestamp.from(expiration));
    }

}