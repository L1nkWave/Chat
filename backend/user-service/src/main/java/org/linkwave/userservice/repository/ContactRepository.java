package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    @Query("""
            select c from ContactEntity c
            join fetch c.user
            where
                c.ownerId=:userId and (
                    upper(c.user.username) like upper(:searchPattern) or
                    upper(c.user.name) like upper(:searchPattern) or
                    upper(c.alias) like upper(:searchPattern)
                )
            """)
    List<ContactEntity> getContactsByUsernameNameAliasContains(Long userId, String searchPattern);

    @Query(value = "select c.* from contacts c where c.user_id_1 = :userId and c.user_id_2 = :userId2", nativeQuery = true)
    Optional<ContactEntity> findContactPair(Long userId, Long userId2);

}
