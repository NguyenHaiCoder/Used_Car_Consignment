package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern such as {@code %ref%}; use {@code %} to match all. */
    @Query(value = """
            select p from Payment p join fetch p.salesContract sc join fetch sc.reservation r join fetch r.buyer b
            where (:status is null or p.status = :status)
              and (lower(p.code) like :pattern
                   or lower(sc.code) like :pattern
                   or lower(coalesce(p.referenceCode, '')) like :pattern
                   or lower(coalesce(p.payerName, '')) like :pattern
                   or lower(b.fullName) like :pattern)
            """,
            countQuery = """
            select count(p) from Payment p join p.salesContract sc join sc.reservation r join r.buyer b
            where (:status is null or p.status = :status)
              and (lower(p.code) like :pattern
                   or lower(sc.code) like :pattern
                   or lower(coalesce(p.referenceCode, '')) like :pattern
                   or lower(coalesce(p.payerName, '')) like :pattern
                   or lower(b.fullName) like :pattern)
            """)
    Page<Payment> search(@Param("pattern") String pattern, @Param("status") PaymentStatus status, Pageable pageable);

    @Query("""
            select p from Payment p join fetch p.salesContract sc join fetch sc.reservation r
            join fetch r.buyer join fetch r.vehicle left join fetch p.paymentTransaction
            where p.id = :id
            """)
    Optional<Payment> findDetailById(@Param("id") Long id);

    /** Payments of a contract, oldest first. */
    @Query("select p from Payment p where p.salesContract.id = :contractId order by p.createdAt")
    List<Payment> findByContract(@Param("contractId") Long contractId);

    /** Payments in the given status with the contract and buyer loaded, newest first. */
    @Query("""
            select p from Payment p join fetch p.salesContract sc join fetch sc.reservation r join fetch r.buyer
            where p.status = :status order by p.createdAt desc
            """)
    List<Payment> findByStatus(@Param("status") PaymentStatus status);

    /** Duplicate reference check: a reference code is recorded once. */
    boolean existsByReferenceCodeIgnoreCase(String referenceCode);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.salesContract.id = :contractId and p.status in :statuses")
    BigDecimal sumByContract(@Param("contractId") Long contractId, @Param("statuses") Collection<PaymentStatus> statuses);

    /** Rows of {status, count} used for the status counters above the list. */
    @Query("select p.status, count(p) from Payment p group by p.status")
    List<Object[]> countByStatus();
}
