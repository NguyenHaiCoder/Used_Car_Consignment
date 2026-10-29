package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.HandoverItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverItemRepository extends JpaRepository<HandoverItem, Long> {

    List<HandoverItem> findByHandoverIdOrderById(Long handoverId);
}
