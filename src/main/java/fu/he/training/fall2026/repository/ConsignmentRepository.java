package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsignmentRepository extends JpaRepository<Consignment, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern such as {@code %camry%}; use {@code %} to match all. */
    @Query(value = """
            select c from Consignment c join fetch c.consignor o join fetch c.vehicle v
            where (:status is null or c.status = :status)
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """,
            countQuery = """
            select count(c) from Consignment c join c.consignor o join c.vehicle v
            where (:status is null or c.status = :status)
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """)
    Page<Consignment> search(
            @Param("pattern") String pattern,
            @Param("status") ConsignmentStatus status,
            Pageable pageable);

    @Query("select c from Consignment c join fetch c.consignor join fetch c.vehicle where c.id = :id")
    Optional<Consignment> findDetailById(@Param("id") Long id);

    /** Rows of {status, count} used for the status counters above the list. */
    @Query("select c.status, count(c) from Consignment c group by c.status")
    List<Object[]> countByStatus();
}
