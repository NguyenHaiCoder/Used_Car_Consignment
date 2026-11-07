package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.sales.Handover;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverRepository extends JpaRepository<Handover, Long> {

    Optional<Handover> findBySalesContractId(Long salesContractId);

    boolean existsBySalesContractIdAndStatusIn(Long salesContractId, Iterable<HandoverStatus> statuses);
}
