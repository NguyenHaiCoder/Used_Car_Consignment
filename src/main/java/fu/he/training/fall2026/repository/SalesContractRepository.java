package fu.he.training.fall2026.repository;
import fu.he.training.fall2026.entity.sales.SalesContract;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.Collection;
import fu.he.training.fall2026.entity.enums.ContractStatus;
public interface SalesContractRepository extends JpaRepository<SalesContract, Long>, JpaSpecificationExecutor<SalesContract> {
    boolean existsByReservation_Buyer_IdAndStatusNotIn(Long buyerId, Collection<ContractStatus> statuses);
    @Query("select s.reservation.id from SalesContract s where s.id = :id")
    Optional<Long> findReservationId(Long id);
    Optional<SalesContract> findByCode(String code);
    Optional<SalesContract> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
}
