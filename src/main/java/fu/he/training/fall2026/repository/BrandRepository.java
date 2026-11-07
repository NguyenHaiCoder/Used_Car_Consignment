package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Brand;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    /** {@code pattern} là LIKE lower-case, ví dụ {@code %hyundai%}; dùng {@code %} để lấy tất cả. */
    @Query("""
            select b from Brand b
            where (:status is null or b.status = :status)
              and (lower(b.name) like :pattern or lower(b.code) like :pattern)
            """)
    Page<Brand> search(
            @Param("pattern") String pattern,
            @Param("status") CatalogStatus status,
            Pageable pageable);

    @Query("select count(b) > 0 from Brand b where lower(b.name) = lower(:name) and b.id <> :excludeId")
    boolean existsDuplicateName(@Param("name") String name, @Param("excludeId") Long excludeId);

    List<Brand> findByStatusOrderByNameAsc(CatalogStatus status);

    Optional<Brand> findByCode(String code);

    @Query("select b.status, count(b) from Brand b group by b.status")
    List<Object[]> countByStatus();
}
