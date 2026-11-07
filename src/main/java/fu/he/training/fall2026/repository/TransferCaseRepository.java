package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferCaseRepository extends JpaRepository<TransferCase, Long> {

    @Query(
            value = """
                    select t from TransferCase t
                    join fetch t.salesContract sc
                    join fetch sc.reservation r
                    join fetch r.buyer b
                    join fetch r.vehicle v
                    left join fetch t.responsibleUser u
                    where (:status is null or t.status = :status)
                      and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
                      and (:fromDate is null or t.deadline >= :fromDate)
                      and (:toDate is null or t.deadline <= :toDate)
                      and (:amountMin is null or sc.salePrice >= :amountMin)
                      and (:amountMax is null or sc.salePrice <= :amountMax)
                      and (lower(t.code) like :pattern
                           or lower(sc.code) like :pattern
                           or lower(b.fullName) like :pattern
                           or lower(b.phone) like :pattern
                           or lower(v.make) like :pattern
                           or lower(v.model) like :pattern
                           or lower(coalesce(v.licensePlate, '')) like :pattern
                           or lower(v.vin) like :pattern)
                    """,
            countQuery = """
                    select count(t) from TransferCase t
                    join t.salesContract sc
                    join sc.reservation r
                    join r.buyer b
                    join r.vehicle v
                    left join t.responsibleUser u
                    where (:status is null or t.status = :status)
                      and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
                      and (:fromDate is null or t.deadline >= :fromDate)
                      and (:toDate is null or t.deadline <= :toDate)
                      and (:amountMin is null or sc.salePrice >= :amountMin)
                      and (:amountMax is null or sc.salePrice <= :amountMax)
                      and (lower(t.code) like :pattern
                           or lower(sc.code) like :pattern
                           or lower(b.fullName) like :pattern
                           or lower(b.phone) like :pattern
                           or lower(v.make) like :pattern
                           or lower(v.model) like :pattern
                           or lower(coalesce(v.licensePlate, '')) like :pattern
                           or lower(v.vin) like :pattern)
                    """)
    Page<TransferCase> search(
            @Param("pattern") String pattern,
            @Param("status") TransferStatus status,
            @Param("staff") String staff,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("amountMin") BigDecimal amountMin,
            @Param("amountMax") BigDecimal amountMax,
            Pageable pageable);

    @Query("""
            select t from TransferCase t
            join fetch t.salesContract sc
            join fetch sc.reservation r
            join fetch r.buyer
            join fetch r.vehicle
            join fetch r.inquiry i
            left join fetch i.assignedTo
            left join fetch r.assignedTo
            left join fetch t.responsibleUser
            where t.id = :id
            """)
    Optional<TransferCase> findDetailById(@Param("id") Long id);

    boolean existsBySalesContractId(Long salesContractId);

    Optional<TransferCase> findBySalesContractId(Long salesContractId);

    @Query("select t.status, count(t) from TransferCase t group by t.status")
    List<Object[]> countByStatus();

    @Query("""
            select distinct u.fullName from TransferCase t
            join t.responsibleUser u
            where u.fullName is not null and u.fullName <> ''
            order by u.fullName
            """)
    List<String> findDistinctStaffNames();
}
