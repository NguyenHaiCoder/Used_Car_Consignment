package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("select i.vehicle.id from Inquiry i where i.id = :id")
    Optional<Long> findVehicleId(@Param("id") Long id);

    @Query("select count(v) > 0 from Viewing v where v.inquiry.id = :id and v.deletedAt is null")
    boolean hasViewings(@Param("id") Long id);

    @Query("select count(r) > 0 from Reservation r where r.inquiry.id = :id")
    boolean hasReservations(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inquiry i where i.id = :id")
    Optional<Inquiry> lockById(@Param("id") Long id);

    @Query(value = """
            select i from Inquiry i join fetch i.buyer b join fetch i.vehicle v
            left join fetch i.assignedTo u
            where (lower(i.code) like :pattern escape '!'
                or cast(i.id as String) like :pattern escape '!'
                or lower(b.fullName) like :pattern escape '!'
                or lower(v.vin) like :pattern escape '!'
                or lower(v.model) like :pattern escape '!')
            and (:status is null or i.status = :status)
            and (:salespersonId is null or u.id = :salespersonId)
            """, countQuery = """
            select count(i) from Inquiry i join i.buyer b join i.vehicle v
            left join i.assignedTo u
            where (lower(i.code) like :pattern escape '!'
                or cast(i.id as String) like :pattern escape '!'
                or lower(b.fullName) like :pattern escape '!'
                or lower(v.vin) like :pattern escape '!'
                or lower(v.model) like :pattern escape '!')
            and (:status is null or i.status = :status)
            and (:salespersonId is null or u.id = :salespersonId)
            """)
    Page<Inquiry> search(@Param("pattern") String pattern, @Param("status") InquiryStatus status,
                         @Param("salespersonId") Long salespersonId, Pageable pageable);

    @Query(value = """
            select i from Inquiry i
            left join fetch i.vehicle v
            left join fetch i.assignedTo u
            where i.buyer.id = :buyerId
            """,
            countQuery = "select count(i) from Inquiry i where i.buyer.id = :buyerId")
    Page<Inquiry> findByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);
}
