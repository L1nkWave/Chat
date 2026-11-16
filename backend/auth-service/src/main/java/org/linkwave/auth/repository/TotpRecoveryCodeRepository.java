package org.linkwave.auth.repository;

import org.linkwave.auth.entity.TotpRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TotpRecoveryCodeRepository extends JpaRepository<TotpRecoveryCode, Long> {

    List<TotpRecoveryCode> findByUserIdAndUsedAtIsNull(Long userId);

    void deleteByUserId(Long userId);

}