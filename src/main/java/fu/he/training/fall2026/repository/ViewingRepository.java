package fu.he.training.fall2026.repository;
import fu.he.training.fall2026.entity.sales.Viewing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface ViewingRepository extends JpaRepository<Viewing, Long>, JpaSpecificationExecutor<Viewing> {
    boolean existsByInquiry_Buyer_IdAndViewingTypeAndStatusAndDeletedAtIsNull(
            Long buyerId, String viewingType, String status);

    List<Viewing> findByInquiryIdOrderByScheduledAtDesc(Long inquiryId);
    boolean existsByInquiryIdAndDeletedAtIsNull(Long inquiryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Viewing v where v.id = :id")
    Optional<Viewing> lockById(Long id);

    @Query("""
        select count(v) > 0 from Viewing v
        where v.id <> :viewingId and v.deletedAt is null and (v.status is null or v.status <> 'CANCELLED')
        and v.scheduledAt < :end and v.endAt > :start
        and coalesce(v.assignedTo.id, v.inquiry.assignedTo.id) = :salespersonId
        """)
    boolean overlapsSalespersonExcept(Long viewingId, Long salespersonId, Instant start, Instant end);

    @Query("""
        select count(v) > 0 from Viewing v
        where v.id <> :viewingId and v.deletedAt is null and (v.status is null or v.status <> 'CANCELLED')
        and v.scheduledAt < :end and v.endAt > :start
        and v.inquiry.vehicle.id = :vehicleId
        """)
    boolean overlapsVehicleExcept(Long viewingId, Long vehicleId, Instant start, Instant end);

    // Use the inquiry salesperson when a viewing has no explicit assignee.
    @Query("""
        select count(v) > 0 from Viewing v
        where v.deletedAt is null and (v.status is null or v.status <> 'CANCELLED')
        and v.scheduledAt < :end and v.endAt > :start
        and coalesce(v.assignedTo.id, v.inquiry.assignedTo.id) = :salespersonId
        """)
    boolean overlapsSalesperson(Long salespersonId, Instant start, Instant end);

    @Query("""
        select count(v) > 0 from Viewing v
        where v.deletedAt is null and (v.status is null or v.status <> 'CANCELLED')
        and v.scheduledAt < :end and v.endAt > :start
        and v.inquiry.vehicle.id = :vehicleId
        """)
    boolean overlapsVehicle(Long vehicleId, Instant start, Instant end);
}
