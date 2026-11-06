package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.InspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {
}
