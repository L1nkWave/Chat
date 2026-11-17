package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.FacultyEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyRepository extends JpaRepository<FacultyEntity, Long> {

    boolean existsByCode(String code);

    /** pattern dạng "%từ khóa%" đã chuyển thành chữ thường; "%%" nghĩa là tất cả. */
    @Query("select f from FacultyEntity f where lower(f.name) like :pattern or lower(f.code) like :pattern order by f.name")
    List<FacultyEntity> search(@Param("pattern") String pattern, Pageable pageable);
}