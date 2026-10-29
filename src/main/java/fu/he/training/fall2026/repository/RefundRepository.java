package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.payment.Refund;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("select r from Refund r join fetch r.payment p where r.id = :id")
    Optional<Refund> findDetailById(@Param("id") Long id);

    /** Refunds of a payment, newest first. */
    @Query("select r from Refund r left join fetch r.reservation where r.payment.id = :paymentId order by r.createdAt desc")
    List<Refund> findByPayment(@Param("paymentId") Long paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status in :statuses")
    BigDecimal sumByPayment(@Param("paymentId") Long paymentId, @Param("statuses") Collection<RefundStatus> statuses);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.salesContract.id = :contractId and r.status in :statuses")
    BigDecimal sumByContract(@Param("contractId") Long contractId, @Param("statuses") Collection<RefundStatus> statuses);

    long countByStatus(RefundStatus status);
}
