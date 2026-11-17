package org.linkwave.userservice.repository;

import org.linkwave.userservice.entity.SchoolClassEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {

    boolean existsByCode(String code);

    /** facultyId = 0 nghĩa là không lọc theo khoa (tránh tham số null với PostgreSQL). */
    @Query("""
            select c from SchoolClassEntity c
            where (lower(c.name) like :pattern or lower(c.code) like :pattern)
              and (:facultyId = 0L or c.faculty.id = :facultyId)
            order by c.code
            """)
    List<SchoolClassEntity> search(@Param("pattern") String pattern,
                                   @Param("facultyId") Long facultyId,
                                   Pageable pageable);
}