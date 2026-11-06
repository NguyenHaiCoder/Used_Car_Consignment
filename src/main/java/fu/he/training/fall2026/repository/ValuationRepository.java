package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Valuation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValuationRepository extends JpaRepository<Valuation, Long> {

    Optional<Valuation> findFirstByConsignmentIdOrderByIdDesc(Long consignmentId);

    Optional<Valuation> findFirstByConsignmentCodeOrderByIdDesc(String consignmentCode);
}
