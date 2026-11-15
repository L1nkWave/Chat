package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Danh mục vai trò cho màn hình quản trị (tạo vai trò, cấp vai trò theo phạm vi). */
@Repository
public interface RoleCatalogRepository extends JpaRepository<RoleEntity, Integer> {

    Optional<RoleEntity> findByName(String name);

    boolean existsByName(String name);

}