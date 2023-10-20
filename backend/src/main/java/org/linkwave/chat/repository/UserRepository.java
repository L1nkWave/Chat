package org.linkwave.chat.repository;

import org.linkwave.chat.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    @Query("select u from UserEntity u join fetch u.roles where u.username = :username")
    Optional<UserEntity> findUserWithRoles(@Param("username") String username);

    Optional<UserEntity> findByRefreshToken(String refreshToken);

}