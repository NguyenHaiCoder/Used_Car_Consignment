package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.ViewingStatus;
import fu.he.training.fall2026.entity.sales.Viewing;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViewingRepository extends JpaRepository<Viewing, Long> {

    @Query("""
            select v from Viewing v
            where v.inquiry.id = :inquiryId
            order by v.scheduledAt desc, v.id desc
            """)
    List<Viewing> findByInquiryIdOrderByLatest(@Param("inquiryId") Long inquiryId);

    @Query("""
            select v from Viewing v
              join fetch v.inquiry i
              join fetch i.vehicle
            where v.id = :id and i.id = :inquiryId
            """)
    Optional<Viewing> findDetail(@Param("inquiryId") Long inquiryId, @Param("id") Long id);

    boolean existsByInquiryIdAndStatusIn(Long inquiryId, Collection<ViewingStatus> statuses);

    boolean existsByInquiryIdAndStatus(Long inquiryId, ViewingStatus status);

    /** Lịch đang hiệu lực của xe — kiểm tra trùng giờ ở service. */
    @Query("""
            select v from Viewing v
              join fetch v.inquiry i
            where i.vehicle.id = :vehicleId
              and v.status in :active
            """)
    List<Viewing> findActiveByVehicle(
            @Param("vehicleId") Long vehicleId,
            @Param("active") Collection<ViewingStatus> active);
}
