package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.sales.Handover;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandoverRepository extends JpaRepository<Handover, Long> {

    @Query("""
            select h from Handover h join fetch h.salesContract sc join fetch sc.reservation r
            join fetch r.buyer join fetch r.vehicle
            where h.id = :id
            """)
    Optional<Handover> findDetailById(@Param("id") Long id);

    /** Every handover with its contract id, used to merge handover status into the queue. */
    @Query("select h from Handover h join fetch h.salesContract")
    List<Handover> findAllWithContract();

    boolean existsBySalesContractId(Long salesContractId);

    long countByStatus(HandoverStatus status);
}
