package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.PayoutStatus;
import fu.he.training.fall2026.entity.payment.Payout;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    @Query(value = """
            select p from Payout p join fetch p.settlement s join fetch s.salesContract
            where (:status is null or p.status = :status)
            """,
            countQuery = "select count(p) from Payout p where (:status is null or p.status = :status)")
    Page<Payout> search(@Param("status") PayoutStatus status, Pageable pageable);

    @Query("select p from Payout p join fetch p.settlement s join fetch s.salesContract where p.id = :id")
    Optional<Payout> findDetailById(@Param("id") Long id);

    /** Payouts of a settlement, newest first. */
    @Query("select p from Payout p where p.settlement.id = :settlementId order by p.createdAt desc")
    List<Payout> findBySettlement(@Param("settlementId") Long settlementId);

    boolean existsBySettlementIdAndStatusIn(Long settlementId, Collection<PayoutStatus> statuses);

    long countByStatusIn(Collection<PayoutStatus> statuses);
}
