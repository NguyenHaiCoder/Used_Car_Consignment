package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleSaleNewsRepository extends JpaRepository<VehicleSaleNews, Long> {
    boolean existsByConsignmentIdAndStatus(Long consignmentId, ListingStatus status);
}
