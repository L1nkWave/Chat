package org.linkwave.auth.repository;

import org.linkwave.auth.entity.TotpSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TotpSecretRepository extends JpaRepository<TotpSecret, Long> {
}