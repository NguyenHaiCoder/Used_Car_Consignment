package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.payment.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query(value = """
            select s from Settlement s join fetch s.salesContract sc join fetch s.consignmentAgreement a
            join fetch a.consignment c join fetch c.consignor
            where (:status is null or s.status = :status)
            """,
            countQuery = "select count(s) from Settlement s where (:status is null or s.status = :status)")
    Page<Settlement> search(@Param("status") SettlementStatus status, Pageable pageable);

    @Query("""
            select s from Settlement s join fetch s.salesContract sc join fetch sc.reservation r join fetch r.vehicle
            join fetch s.consignmentAgreement a join fetch a.consignment c join fetch c.consignor
            where s.id = :id
            """)
    Optional<Settlement> findDetailById(@Param("id") Long id);

    Optional<Settlement> findBySalesContractId(Long salesContractId);

    /** Rows of {status, count} used for the status counters above the list. */
    @Query("select s.status, count(s) from Settlement s group by s.status")
    List<Object[]> countByStatus();
}
