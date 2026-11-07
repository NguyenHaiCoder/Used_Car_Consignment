package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsBySalesContractIdAndStatus(Long salesContractId, PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.salesContract.id = :contractId")
    BigDecimal sumCollected(@Param("contractId") Long contractId);

    boolean existsByReferenceCodeIgnoreCase(String referenceCode);

    List<Payment> findBySalesContractIdOrderByPaidAtAscIdAsc(Long salesContractId);

    List<Payment> findByReservationIdOrderByPaidAtAscIdAsc(Long reservationId);
}
