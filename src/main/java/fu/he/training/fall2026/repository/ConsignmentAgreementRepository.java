package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsignmentAgreementRepository extends JpaRepository<ConsignmentAgreement, Long> {

    /** The newest version of the consignment's agreement. */
    Optional<ConsignmentAgreement> findFirstByConsignmentIdOrderByVersionNoDesc(Long consignmentId);

    List<ConsignmentAgreement> findByConsignmentIdOrderByVersionNoDesc(Long consignmentId);
}
