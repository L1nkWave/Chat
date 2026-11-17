package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Tham số luôn có giá trị để tránh lỗi kiểu dữ liệu của PostgreSQL:
     * action/targetId = "" nghĩa là không lọc; actorId = 0 nghĩa là không lọc.
     * Khoảng thời gian: từ from (gồm) đến to (không gồm).
     */
    @Query(
            value = """
                    select a from AuditLogEntity a
                    where (:action = '' or a.action = :action)
                      and (:actorId = 0L or a.actorId = :actorId)
                      and (:targetId = '' or a.targetId = :targetId)
                      and a.createdAt >= :from
                      and a.createdAt < :to
                    """,
            countQuery = """
                    select count(a) from AuditLogEntity a
                    where (:action = '' or a.action = :action)
                      and (:actorId = 0L or a.actorId = :actorId)
                      and (:targetId = '' or a.targetId = :targetId)
                      and a.createdAt >= :from
                      and a.createdAt < :to
                    """
    )
    Page<AuditLogEntity> search(@Param("action") String action,
                                @Param("actorId") Long actorId,
                                @Param("targetId") String targetId,
                                @Param("from") ZonedDateTime from,
                                @Param("to") ZonedDateTime to,
                                Pageable pageable);
}