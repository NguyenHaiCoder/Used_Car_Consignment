package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.InspectionType;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionTypeRepository extends JpaRepository<InspectionType, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern matched against the name and the vehicle type label. */
    @Query(value = """
            select t from InspectionType t join fetch t.vehicleType v
            where (:status is null or t.status = :status)
              and (lower(t.name) like :pattern or lower(v.label) like :pattern)
            """,
            countQuery = """
            select count(t) from InspectionType t join t.vehicleType v
            where (:status is null or t.status = :status)
              and (lower(t.name) like :pattern or lower(v.label) like :pattern)
            """)
    Page<InspectionType> search(
            @Param("pattern") String pattern,
            @Param("status") CatalogStatus status,
            Pageable pageable);

    @Query("select t from InspectionType t join fetch t.vehicleType where t.id = :id")
    Optional<InspectionType> findDetailById(@Param("id") Long id);

    @Query("select t from InspectionType t join fetch t.vehicleType where t.status = :status order by t.id")
    List<InspectionType> findAllByStatusWithVehicleType(@Param("status") CatalogStatus status);

    /** True when another inspection type (id different from {@code excludeId}) has the same name, ignoring case. */
    @Query("select count(t) > 0 from InspectionType t where lower(t.name) = lower(:name) and t.id <> :excludeId")
    boolean existsDuplicateName(@Param("name") String name, @Param("excludeId") Long excludeId);

    boolean existsByVehicleTypeIdAndStatus(Long vehicleTypeId, CatalogStatus status);

    /** Rows of {status, count} used for the counters above the list. */
    @Query("select t.status, count(t) from InspectionType t group by t.status")
    List<Object[]> countByStatus();
}
