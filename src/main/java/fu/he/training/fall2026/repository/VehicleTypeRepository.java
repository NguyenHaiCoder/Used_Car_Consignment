package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.VehicleType;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern such as {@code %suv%}; use {@code %} to match all. */
    @Query("""
            select t from VehicleType t
            where (:status is null or t.status = :status)
              and (lower(t.code) like :pattern or lower(t.label) like :pattern)
            """)
    Page<VehicleType> search(
            @Param("pattern") String pattern,
            @Param("status") CatalogStatus status,
            Pageable pageable);

    /** True when another vehicle type (id different from {@code excludeId}) already uses {@code code}. */
    @Query("select count(t) > 0 from VehicleType t where t.code = :code and t.id <> :excludeId")
    boolean existsDuplicateCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    List<VehicleType> findByStatusOrderByLabelAsc(CatalogStatus status);

    Optional<VehicleType> findByCode(String code);

    /** Rows of {status, count} used for the counters above the list. */
    @Query("select t.status, count(t) from VehicleType t group by t.status")
    List<Object[]> countByStatus();
}
