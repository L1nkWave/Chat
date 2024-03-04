package org.linkwave.auth.repository;

import org.linkwave.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("from User u join fetch u.roles where u.username = :username")
    Optional<User> findByUsername(String username);

}
