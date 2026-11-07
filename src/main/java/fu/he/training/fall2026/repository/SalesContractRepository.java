package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesContractRepository extends JpaRepository<SalesContract, Long> {

    @Query("""
            select sc from SalesContract sc
            join fetch sc.reservation r
            join fetch r.buyer
            join fetch r.vehicle
            join fetch r.inquiry i
            left join fetch i.assignedTo
            left join fetch r.assignedTo
            where sc.id = :id
            """)
    Optional<SalesContract> findDetailById(@Param("id") Long id);

    @Query("""
            select sc from SalesContract sc
            join fetch sc.reservation r
            join fetch r.buyer
            join fetch r.vehicle
            join fetch r.inquiry i
            left join fetch i.assignedTo
            left join fetch r.assignedTo
            where sc.signedOn is not null
              and sc.status <> :cancelled
              and not exists (select 1 from TransferCase t where t.salesContract = sc)
            order by sc.id desc
            """)
    List<SalesContract> findSignedWithoutTransfer(@Param("cancelled") ContractStatus cancelled);

    /** Hợp đồng đã gửi yêu cầu thu (có PaymentRequest). */
    @Query(value = """
            select sc from SalesContract sc
              join fetch sc.reservation r
              join fetch r.buyer
              join fetch r.vehicle
              left join fetch r.assignedTo u
            where exists (
                  select 1 from PaymentRequest pr
                  where pr.salesContract = sc
                    and (:paymentStatus is null or pr.status = :paymentStatus))
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or sc.createdAt >= :createdFrom)
              and (:createdTo is null or sc.createdAt < :createdTo)
              and (:amountMin is null or sc.salePrice >= :amountMin)
              and (:amountMax is null or sc.salePrice <= :amountMax)
              and (lower(sc.code) like :pattern
                   or lower(r.buyer.fullName) like :pattern
                   or lower(r.buyer.phone) like :pattern
                   or lower(r.vehicle.make) like :pattern
                   or lower(r.vehicle.model) like :pattern
                   or lower(coalesce(r.vehicle.licensePlate, '')) like :pattern
                   or lower(r.vehicle.vin) like :pattern)
            """,
            countQuery = """
            select count(sc) from SalesContract sc
              join sc.reservation r
              join r.buyer
              join r.vehicle
              left join r.assignedTo u
            where exists (
                  select 1 from PaymentRequest pr
                  where pr.salesContract = sc
                    and (:paymentStatus is null or pr.status = :paymentStatus))
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or sc.createdAt >= :createdFrom)
              and (:createdTo is null or sc.createdAt < :createdTo)
              and (:amountMin is null or sc.salePrice >= :amountMin)
              and (:amountMax is null or sc.salePrice <= :amountMax)
              and (lower(sc.code) like :pattern
                   or lower(r.buyer.fullName) like :pattern
                   or lower(r.buyer.phone) like :pattern
                   or lower(r.vehicle.make) like :pattern
                   or lower(r.vehicle.model) like :pattern
                   or lower(coalesce(r.vehicle.licensePlate, '')) like :pattern
                   or lower(r.vehicle.vin) like :pattern)
            """)
    Page<SalesContract> searchWithPaymentRequest(
            @Param("pattern") String pattern,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("staff") String staff,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("amountMin") BigDecimal amountMin,
            @Param("amountMax") BigDecimal amountMax,
            Pageable pageable);

    @Query("""
            select distinct u.fullName from SalesContract sc
              join sc.reservation r
              join r.assignedTo u
            where exists (select 1 from PaymentRequest pr where pr.salesContract = sc)
              and u.fullName is not null and u.fullName <> ''
            order by u.fullName
            """)
    List<String> findPaymentDealStaffNames();

    @Query("""
            select sc from SalesContract sc
              join fetch sc.reservation r
              join fetch r.buyer
              join fetch r.vehicle
              left join fetch r.assignedTo
            where sc.signedOn is not null
              and sc.status <> fu.he.training.fall2026.entity.enums.ContractStatus.CANCELLED
            order by sc.id desc
            """)
    List<SalesContract> findAllSignedActive();
}
