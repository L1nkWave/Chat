package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Integer> {

    List<PermissionEntity> findByCodeIn(Collection<String> codes);

}