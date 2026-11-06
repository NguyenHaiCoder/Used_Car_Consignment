package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleSaleNewsRepository extends JpaRepository<VehicleSaleNews, Long> {

    /**
     * Quản lý tin đăng. {@code pattern} is a lower-cased LIKE pattern matched against the listing code, the
     * consignment code, the car, the plate and the owner; every other filter is skipped when null.
     * {@code createdTo} is exclusive.
     */
    @Query(value = """
            select n from VehicleSaleNews n
              join fetch n.consignment c join fetch c.vehicle v join fetch c.consignor o
              left join fetch n.assignedTo u
            where (:status is null or n.status = :status)
              and (:staffId is null or u.id = :staffId)
              and (:createdFrom is null or n.createdAt >= :createdFrom)
              and (:createdTo is null or n.createdAt < :createdTo)
              and (:priceMin is null or n.listPrice >= :priceMin)
              and (:priceMax is null or n.listPrice <= :priceMax)
              and (lower(n.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern
                   or lower(o.fullName) like :pattern)
            """,
            countQuery = """
            select count(n) from VehicleSaleNews n
              join n.consignment c join c.vehicle v join c.consignor o
              left join n.assignedTo u
            where (:status is null or n.status = :status)
              and (:staffId is null or u.id = :staffId)
              and (:createdFrom is null or n.createdAt >= :createdFrom)
              and (:createdTo is null or n.createdAt < :createdTo)
              and (:priceMin is null or n.listPrice >= :priceMin)
              and (:priceMax is null or n.listPrice <= :priceMax)
              and (lower(n.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern
                   or lower(o.fullName) like :pattern)
            """)
    Page<VehicleSaleNews> search(
            @Param("pattern") String pattern,
            @Param("status") ListingStatus status,
            @Param("staffId") Long staffId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            Pageable pageable);

    /** Rows of {status, count} used for the counters above the list. */
    @Query("select n.status, count(n) from VehicleSaleNews n group by n.status")
    List<Object[]> countByStatus();

    @Query("""
            select n from VehicleSaleNews n
              join fetch n.consignment c join fetch c.vehicle join fetch c.consignor
              left join fetch n.assignedTo
            where n.id = :id
            """)
    Optional<VehicleSaleNews> findDetailById(@Param("id") Long id);

    /** Staff who own at least one listing, for the "Người phụ trách" filter. */
    @Query("select distinct u from VehicleSaleNews n join n.assignedTo u order by u.fullName")
    List<User> findStaffs();

    /**
     * Tin bán xe (public). {@code pattern} is matched against the title, the car, the year and the fuel;
     * {@code brand} and the price bounds are skipped when null, {@code priceMax} is exclusive.
     */
    @Query(value = """
            select n from VehicleSaleNews n join fetch n.consignment c join fetch c.vehicle v
            where n.status = :status
              and (:brand is null or v.make = :brand)
              and (:priceMin is null or n.listPrice >= :priceMin)
              and (:priceMax is null or n.listPrice < :priceMax)
              and (lower(n.title) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or cast(v.year as string) like :pattern
                   or lower(coalesce(v.fuelType, '')) like :pattern)
            """,
            countQuery = """
            select count(n) from VehicleSaleNews n join n.consignment c join c.vehicle v
            where n.status = :status
              and (:brand is null or v.make = :brand)
              and (:priceMin is null or n.listPrice >= :priceMin)
              and (:priceMax is null or n.listPrice < :priceMax)
              and (lower(n.title) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or cast(v.year as string) like :pattern
                   or lower(coalesce(v.fuelType, '')) like :pattern)
            """)
    Page<VehicleSaleNews> searchPublic(
            @Param("pattern") String pattern,
            @Param("status") ListingStatus status,
            @Param("brand") String brand,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            Pageable pageable);

    @Query("""
            select distinct v.make from VehicleSaleNews n join n.consignment c join c.vehicle v
            where n.status = :status
            order by v.make
            """)
    List<String> findBrands(@Param("status") ListingStatus status);

    /** Newest listings in the given status with their vehicle; the page size is the number of cards. */
    @Query("""
            select n from VehicleSaleNews n join fetch n.consignment c join fetch c.vehicle
            where n.status = :status
            order by n.publishedAt desc, n.id desc
            """)
    List<VehicleSaleNews> findNewest(@Param("status") ListingStatus status, Pageable pageable);
}
