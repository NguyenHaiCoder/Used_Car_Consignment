package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsBySalesContractIdAndStatus(Long salesContractId, PaymentStatus status);
}
