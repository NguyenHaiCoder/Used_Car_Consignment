package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.payment.PaymentRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    Optional<PaymentRequest> findBySalesContractId(Long salesContractId);

    boolean existsBySalesContractId(Long salesContractId);
}
