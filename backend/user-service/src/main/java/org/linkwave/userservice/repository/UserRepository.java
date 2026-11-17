package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    @Query(
            value = """
                    select count(*)
                    from users
                    where id != :requestUserId
                        and id not in (select c.user_id_2 as cid
                                        from users u
                                        join contacts c on u.id = c.user_id_1
                                        where u.id = :requestUserId)
                        and upper(username) like upper(:usernamePattern)
                    """,
            nativeQuery = true
    )
    long getUsersCountByUsernameContains(Long requestUserId,
                                         String usernamePattern);

    @Query(
            value = """
                    select *
                    from users
                    where id != :requestUserId
                        and id not in (select c.user_id_2 as cid
                                        from users u
                                        join contacts c on u.id = c.user_id_1
                                        where u.id = :requestUserId)
                        and upper(username) like upper(:usernamePattern)
                    order by name
                    offset :offset limit :limit
                    """,
            nativeQuery = true
    )
    List<UserEntity> getUsersByUsernameContains(Long requestUserId, String usernamePattern,
                                                int offset, int limit);

    @Query("select u from UserEntity u join fetch u.roles where u.id = :id")
    Optional<UserEntity> findUserWithRoles(Long id);

    /**
     * Tìm người dùng cho màn hình quản trị.
     * Tham số luôn có giá trị (chuỗi rỗng khi không lọc) để tránh lỗi kiểu dữ liệu của PostgreSQL.
     * pattern: đã chuyển thành chữ thường, dạng %từ khóa%
     * role: tên vai trò toàn trường, "" nghĩa là mọi vai trò
     * status: ALL | LOCKED | ACTIVE
     */
    @Query(
            value = """
                    select distinct u from UserEntity u
                    left join u.roles r
                    where u.isDeleted = false
                      and (lower(u.username) like :pattern or lower(u.name) like :pattern)
                      and (:role = '' or r.name = :role)
                      and (:status = 'ALL'
                           or (:status = 'LOCKED' and u.isBlocked = true)
                           or (:status = 'ACTIVE' and u.isBlocked = false))
                    """,
            countQuery = """
                    select count(distinct u) from UserEntity u
                    left join u.roles r
                    where u.isDeleted = false
                      and (lower(u.username) like :pattern or lower(u.name) like :pattern)
                      and (:role = '' or r.name = :role)
                      and (:status = 'ALL'
                           or (:status = 'LOCKED' and u.isBlocked = true)
                           or (:status = 'ACTIVE' and u.isBlocked = false))
                    """
    )
    Page<UserEntity> searchForAdmin(@Param("pattern") String pattern,
                                    @Param("role") String role,
                                    @Param("status") String status,
                                    Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "delete from user_roles", nativeQuery = true)
    void dropUserRoles();

}