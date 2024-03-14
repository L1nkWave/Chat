package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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
                        and starts_with(username, :username)
                    """,
            nativeQuery = true
    )
    long getUsersCountByUsernameStartsWith(Long requestUserId,
                                           String username);

    @Query(
            value = """
                    select *
                    from users
                    where id != :requestUserId
                        and id not in (select c.user_id_2 as cid
                                        from users u
                                        join contacts c on u.id = c.user_id_1
                                        where u.id = :requestUserId)
                        and starts_with(username, :username)
                    offset :offset limit :limit
                    """,
            nativeQuery = true
    )
    List<UserEntity> getUsersByUsernameStartsWith(Long requestUserId,
                                                  String username,
                                                  int offset,
                                                  int limit);

    @Query("select u from UserEntity u join fetch u.roles where u.id = :id")
    Optional<UserEntity> findUserWithRoles(Long id);

    @Transactional
    @Modifying
    @Query(value = "delete from user_roles", nativeQuery = true)
    void dropUserRoles();

}