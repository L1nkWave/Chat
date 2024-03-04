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
                    from users u
                    where starts_with(u.username, :username) and u.username != :requestUsername
                    """,
            nativeQuery = true
    )
    long getUsersCountByUsernameStartsWith(String requestUsername,
                                           String username);

    @Query(
            value = """
                    select u.*
                    from users u
                    where starts_with(u.username, :username) and u.username != :requestUsername
                    offset :offset
                    limit :limit
                    """,
            nativeQuery = true
    )
    List<UserEntity> getUsersByUsernameStartsWith(String requestUsername,
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