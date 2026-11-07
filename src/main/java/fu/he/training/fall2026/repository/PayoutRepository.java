package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.payment.Payout;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findFirstBySettlementIdOrderByIdDesc(Long settlementId);
}
