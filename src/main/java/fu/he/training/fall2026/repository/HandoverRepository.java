package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.sales.Handover;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandoverRepository extends JpaRepository<Handover, Long> {

    Optional<Handover> findBySalesContractId(Long salesContractId);

    boolean existsBySalesContractIdAndStatusIn(Long salesContractId, Iterable<HandoverStatus> statuses);

    @Query("""
            select h from Handover h
              join fetch h.salesContract c
              join fetch c.reservation r
              join fetch r.buyer
              join fetch r.vehicle
              left join fetch r.assignedTo
              left join fetch h.handedOverBy
            where c.id = :contractId
            """)
    Optional<Handover> findDetailByContractId(@Param("contractId") Long contractId);

    @Query(value = """
            select h from Handover h
              join fetch h.salesContract c
              join fetch c.reservation r
              join fetch r.buyer
              join fetch r.vehicle
              left join fetch r.assignedTo u
              left join fetch h.handedOverBy
            where (:status is null or h.status = :status)
              and (:staff is null or :staff = ''
                   or lower(coalesce(h.handedOverBy.fullName, u.fullName)) = lower(:staff))
              and (:fromDate is null or c.signedOn >= :fromDate)
              and (:toDate is null or c.signedOn <= :toDate)
              and (:amountMin is null or c.salePrice >= :amountMin)
              and (:amountMax is null or c.salePrice <= :amountMax)
              and (lower(h.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(r.buyer.fullName) like :pattern
                   or lower(r.buyer.phone) like :pattern
                   or lower(r.vehicle.make) like :pattern
                   or lower(r.vehicle.model) like :pattern
                   or lower(coalesce(r.vehicle.licensePlate, '')) like :pattern)
            """,
            countQuery = """
            select count(h) from Handover h
              join h.salesContract c
              join c.reservation r
              join r.buyer
              join r.vehicle
              left join r.assignedTo u
              left join h.handedOverBy hb
            where (:status is null or h.status = :status)
              and (:staff is null or :staff = ''
                   or lower(coalesce(hb.fullName, u.fullName)) = lower(:staff))
              and (:fromDate is null or c.signedOn >= :fromDate)
              and (:toDate is null or c.signedOn <= :toDate)
              and (:amountMin is null or c.salePrice >= :amountMin)
              and (:amountMax is null or c.salePrice <= :amountMax)
              and (lower(h.code) like :pattern
                   or lower(c.code) like :pattern
                   or lower(r.buyer.fullName) like :pattern
                   or lower(r.buyer.phone) like :pattern
                   or lower(r.vehicle.make) like :pattern
                   or lower(r.vehicle.model) like :pattern
                   or lower(coalesce(r.vehicle.licensePlate, '')) like :pattern)
            """)
    Page<Handover> search(
            @Param("pattern") String pattern,
            @Param("status") HandoverStatus status,
            @Param("staff") String staff,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("amountMin") BigDecimal amountMin,
            @Param("amountMax") BigDecimal amountMax,
            Pageable pageable);

    @Query("select h.status, count(h) from Handover h group by h.status")
    List<Object[]> countByStatus();

    @Query("""
            select distinct u.fullName from Handover h
              join h.salesContract c join c.reservation r join r.assignedTo u
            where u.fullName is not null and u.fullName <> ''
            order by u.fullName
            """)
    List<String> findAssignedStaffNames();

    @Query("""
            select distinct hb.fullName from Handover h
              join h.handedOverBy hb
            where hb.fullName is not null and hb.fullName <> ''
            order by hb.fullName
            """)
    List<String> findHandedOverStaffNames();
}
