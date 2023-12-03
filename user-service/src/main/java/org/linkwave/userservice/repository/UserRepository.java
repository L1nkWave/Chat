package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    @Query("select u from UserEntity u join fetch u.roles where u.username = :username")
    Optional<UserEntity> findUserWithRoles(@Param("username") String username);

    @Query("select u from UserEntity u join fetch u.roles where u.id = :id")
    Optional<UserEntity> findUserWithRoles(@Param("id") long id);

    Optional<UserEntity> findByRefreshToken(String refreshToken);

    @Transactional
    @Modifying
    @Query(value = "delete from user_roles", nativeQuery = true)
    void dropUserRoles();

}