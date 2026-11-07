package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.VehicleModel;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    /**
     * {@code pattern} là LIKE lower-case; {@code brandId} null = mọi hãng;
     * {@code status} null = mọi trạng thái.
     */
    @Query(
            value = """
                    select m from VehicleModel m join fetch m.brand b
                    where (:status is null or m.status = :status)
                      and (:brandId is null or b.id = :brandId)
                      and (lower(m.name) like :pattern
                           or lower(b.name) like :pattern
                           or lower(m.code) like :pattern)
                    """,
            countQuery = """
                    select count(m) from VehicleModel m join m.brand b
                    where (:status is null or m.status = :status)
                      and (:brandId is null or b.id = :brandId)
                      and (lower(m.name) like :pattern
                           or lower(b.name) like :pattern
                           or lower(m.code) like :pattern)
                    """)
    Page<VehicleModel> search(
            @Param("pattern") String pattern,
            @Param("status") CatalogStatus status,
            @Param("brandId") Long brandId,
            Pageable pageable);

    @Query("""
            select count(m) > 0 from VehicleModel m
            where m.brand.id = :brandId and lower(m.name) = lower(:name) and m.id <> :excludeId
            """)
    boolean existsDuplicateName(
            @Param("brandId") Long brandId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    @Query("select m from VehicleModel m join fetch m.brand where m.id = :id")
    Optional<VehicleModel> findDetailById(@Param("id") Long id);

    long countByBrandId(Long brandId);

    @Query("select count(m) > 0 from VehicleModel m where m.brand.id = :brandId and m.status = :status")
    boolean existsByBrandIdAndStatus(@Param("brandId") Long brandId, @Param("status") CatalogStatus status);

    List<VehicleModel> findByBrandIdAndStatusOrderByNameAsc(Long brandId, CatalogStatus status);

    @Query("""
            select m from VehicleModel m join fetch m.brand b
            where m.status = :status
            order by b.name asc, m.name asc
            """)
    List<VehicleModel> findByStatusWithBrandOrderByNameAsc(@Param("status") CatalogStatus status);

    @Query("select m.status, count(m) from VehicleModel m group by m.status")
    List<Object[]> countByStatus();
}
