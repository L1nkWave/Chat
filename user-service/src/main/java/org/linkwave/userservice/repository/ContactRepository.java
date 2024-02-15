package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    @Query("""
            select c from ContactEntity c
            join fetch c.user
            where c.ownerId=:userId and c.user.username like :usernamePattern
            """)
    List<ContactEntity> getContactsByUsernameStartsWith(Long userId, String usernamePattern);

}
