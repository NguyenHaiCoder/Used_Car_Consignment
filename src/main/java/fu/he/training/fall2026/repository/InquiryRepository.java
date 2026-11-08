package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("""
            select i from Inquiry i
              join fetch i.vehicle v
              left join fetch i.assignedTo
            where i.id = :id
            """)
    Optional<Inquiry> findDetailById(@Param("id") Long id);

    @Query(value = """
            select i from Inquiry i
              join fetch i.vehicle v
              left join fetch i.assignedTo u
            where (:status is null or i.status = :status)
              and (:channel is null or :channel = '' or i.channel = :channel)
              and (:need is null or :need = '' or i.need = :need)
              and (:createdFrom is null or i.createdAt >= :createdFrom)
              and (:createdTo is null or i.createdAt < :createdTo)
              and (:viewing is null or exists (
                    select 1 from Viewing w
                    where w.inquiry = i
                      and w.status = :viewing
                      and w.id = (
                          select max(w2.id) from Viewing w2 where w2.inquiry = i)))
              and (lower(i.code) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern
                   or lower(coalesce(i.phone, '')) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern)
            """,
            countQuery = """
            select count(i) from Inquiry i
              join i.vehicle v
            where (:status is null or i.status = :status)
              and (:channel is null or :channel = '' or i.channel = :channel)
              and (:need is null or :need = '' or i.need = :need)
              and (:createdFrom is null or i.createdAt >= :createdFrom)
              and (:createdTo is null or i.createdAt < :createdTo)
              and (:viewing is null or exists (
                    select 1 from Viewing w
                    where w.inquiry = i
                      and w.status = :viewing
                      and w.id = (
                          select max(w2.id) from Viewing w2 where w2.inquiry = i)))
              and (lower(i.code) like :pattern
                   or lower(coalesce(i.fullName, '')) like :pattern
                   or lower(coalesce(i.phone, '')) like :pattern
                   or lower(concat(v.make, ' ', v.model)) like :pattern)
            """)
    Page<Inquiry> search(
            @Param("pattern") String pattern,
            @Param("status") InquiryStatus status,
            @Param("channel") String channel,
            @Param("need") String need,
            @Param("viewing") ViewingStatus viewing,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            Pageable pageable);

    long countByStatus(InquiryStatus status);

    @Query("""
            select i from Inquiry i
              join fetch i.vehicle v
              left join fetch i.assignedTo
            where v.id = :vehicleId
              and i.status in :statuses
            order by i.id desc
            """)
    List<Inquiry> findHoldableByVehicle(
            @Param("vehicleId") Long vehicleId,
            @Param("statuses") List<InquiryStatus> statuses);

    @Query("""
            select i from Inquiry i
              join fetch i.vehicle
            where i.phone = :phone
            order by i.id desc
            """)
    List<Inquiry> findByPhoneOrderByIdDesc(@Param("phone") String phone);

    @Query("""
            select i from Inquiry i
            where i.phone = :phone
              and i.vehicle.id = :vehicleId
              and i.status not in (
                  fu.he.training.fall2026.entity.enums.InquiryStatus.CLOSED,
                  fu.he.training.fall2026.entity.enums.InquiryStatus.CONVERTED)
            order by i.id desc
            """)
    List<Inquiry> findOpenByPhoneAndVehicle(
            @Param("phone") String phone, @Param("vehicleId") Long vehicleId);

    /** Form public: chặn gửi lại khi đã có liên hệ (kể cả đã chuyển đặt cọc), trừ khi đã đóng. */
    @Query("""
            select i from Inquiry i
            where i.phone = :phone
              and i.vehicle.id = :vehicleId
              and i.status <> fu.he.training.fall2026.entity.enums.InquiryStatus.CLOSED
            order by i.id desc
            """)
    List<Inquiry> findActiveByPhoneAndVehicle(
            @Param("phone") String phone, @Param("vehicleId") Long vehicleId);
}
