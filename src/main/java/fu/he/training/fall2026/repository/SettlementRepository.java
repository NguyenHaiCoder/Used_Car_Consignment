package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.payment.Settlement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsBySalesContractId(Long salesContractId);

    Optional<Settlement> findBySalesContractId(Long salesContractId);

    @Query("""
            select s from Settlement s
              join fetch s.salesContract c
              join fetch c.reservation r
              join fetch r.vehicle v
              join fetch r.inquiry
              left join fetch r.assignedTo
              join fetch s.consignmentAgreement a
              join fetch a.consignment cg
              join fetch cg.consignor
            where s.id = :id
            """)
    Optional<Settlement> findDetailById(@Param("id") Long id);

    @Query(value = """
            select s from Settlement s
              join fetch s.salesContract c
              join fetch c.reservation r
              join fetch r.vehicle v
              join fetch r.inquiry i
              left join fetch r.assignedTo u
              join fetch s.consignmentAgreement a
              join fetch a.consignment cg
              join fetch cg.consignor
            where (:status is null or s.status = :status)
              and (:excludeDraft = false or s.status <> fu.he.training.fall2026.entity.enums.SettlementStatus.DRAFT)
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or s.createdAt >= :createdFrom)
              and (:createdTo is null or s.createdAt < :createdTo)
              and (:amountMin is null or s.netPayoutAmount >= :amountMin)
              and (:amountMax is null or s.netPayoutAmount <= :amountMax)
              and (lower(s.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or lower(cg.code) like :pattern
                   or lower(cg.consignor.fullName) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern)
            """,
            countQuery = """
            select count(s) from Settlement s
              join s.salesContract c
              join c.reservation r
              join r.vehicle v
              join r.inquiry i
              left join r.assignedTo u
              join s.consignmentAgreement a
              join a.consignment cg
              join cg.consignor
            where (:status is null or s.status = :status)
              and (:excludeDraft = false or s.status <> fu.he.training.fall2026.entity.enums.SettlementStatus.DRAFT)
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or s.createdAt >= :createdFrom)
              and (:createdTo is null or s.createdAt < :createdTo)
              and (:amountMin is null or s.netPayoutAmount >= :amountMin)
              and (:amountMax is null or s.netPayoutAmount <= :amountMax)
              and (lower(s.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern
                   or lower(cg.code) like :pattern
                   or lower(cg.consignor.fullName) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern)
            """)
    Page<Settlement> search(
            @Param("pattern") String pattern,
            @Param("status") SettlementStatus status,
            @Param("excludeDraft") boolean excludeDraft,
            @Param("staff") String staff,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("amountMin") BigDecimal amountMin,
            @Param("amountMax") BigDecimal amountMax,
            Pageable pageable);

    @Query("select s.status, count(s) from Settlement s group by s.status")
    List<Object[]> countByStatus();

    @Query("""
            select distinct u.fullName from Settlement s
              join s.salesContract c
              join c.reservation r
              join r.assignedTo u
            where u.fullName is not null and u.fullName <> ''
            order by u.fullName
            """)
    List<String> findDistinctStaffNames();
}
