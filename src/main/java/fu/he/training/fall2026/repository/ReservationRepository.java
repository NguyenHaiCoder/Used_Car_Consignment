package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Reservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select r from Reservation r
              join fetch r.inquiry
              join fetch r.vehicle
              left join fetch r.assignedTo
            where r.id = :id
            """)
    Optional<Reservation> findDetailById(@Param("id") Long id);

    /** Phiếu hủy/hết hạn đã chọn Hoàn cọc — Tài chính xử lý. */
    @Query("""
            select r from Reservation r
              join fetch r.inquiry
              join fetch r.vehicle
              left join fetch r.assignedTo
            where upper(coalesce(r.depositDecision, '')) = 'REFUND'
              and r.status in (
                  fu.he.training.fall2026.entity.enums.ReservationStatus.CANCELLED,
                  fu.he.training.fall2026.entity.enums.ReservationStatus.EXPIRED)
            order by r.id desc
            """)
    List<Reservation> findRefundable();

    @Query(value = """
            select r from Reservation r
              join fetch r.inquiry i
              join fetch r.vehicle v
              left join fetch r.assignedTo u
            where upper(coalesce(r.depositDecision, '')) = 'REFUND'
              and r.status in (
                  fu.he.training.fall2026.entity.enums.ReservationStatus.CANCELLED,
                  fu.he.training.fall2026.entity.enums.ReservationStatus.EXPIRED)
              and (:processed is null
                   or (:processed = true and exists (
                        select 1 from Refund rf
                        where rf.reservation = r
                          and rf.status = fu.he.training.fall2026.entity.enums.RefundStatus.PROCESSED))
                   or (:processed = false and not exists (
                        select 1 from Refund rf
                        where rf.reservation = r
                          and rf.status = fu.he.training.fall2026.entity.enums.RefundStatus.PROCESSED)))
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or r.createdAt >= :createdFrom)
              and (:createdTo is null or r.createdAt < :createdTo)
              and (:amountMin is null or r.depositAmount >= :amountMin)
              and (:amountMax is null or r.depositAmount <= :amountMax)
              and (lower(r.code) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern
                   or lower(coalesce(i.phone, '')) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern)
            """,
            countQuery = """
            select count(r) from Reservation r
              join r.inquiry i
              join r.vehicle v
              left join r.assignedTo u
            where upper(coalesce(r.depositDecision, '')) = 'REFUND'
              and r.status in (
                  fu.he.training.fall2026.entity.enums.ReservationStatus.CANCELLED,
                  fu.he.training.fall2026.entity.enums.ReservationStatus.EXPIRED)
              and (:processed is null
                   or (:processed = true and exists (
                        select 1 from Refund rf
                        where rf.reservation = r
                          and rf.status = fu.he.training.fall2026.entity.enums.RefundStatus.PROCESSED))
                   or (:processed = false and not exists (
                        select 1 from Refund rf
                        where rf.reservation = r
                          and rf.status = fu.he.training.fall2026.entity.enums.RefundStatus.PROCESSED)))
              and (:staff is null or :staff = '' or lower(u.fullName) = lower(:staff))
              and (:createdFrom is null or r.createdAt >= :createdFrom)
              and (:createdTo is null or r.createdAt < :createdTo)
              and (:amountMin is null or r.depositAmount >= :amountMin)
              and (:amountMax is null or r.depositAmount <= :amountMax)
              and (lower(r.code) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern
                   or lower(coalesce(i.phone, '')) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern
                   or lower(coalesce(v.licensePlate, '')) like :pattern)
            """)
    Page<Reservation> searchRefundable(
            @Param("pattern") String pattern,
            @Param("processed") Boolean processed,
            @Param("staff") String staff,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("amountMin") BigDecimal amountMin,
            @Param("amountMax") BigDecimal amountMax,
            Pageable pageable);

    @Query("""
            select distinct u.fullName from Reservation r
              join r.assignedTo u
            where upper(coalesce(r.depositDecision, '')) = 'REFUND'
              and r.status in (
                  fu.he.training.fall2026.entity.enums.ReservationStatus.CANCELLED,
                  fu.he.training.fall2026.entity.enums.ReservationStatus.EXPIRED)
              and u.fullName is not null and u.fullName <> ''
            order by u.fullName
            """)
    List<String> findRefundStaffNames();
}
