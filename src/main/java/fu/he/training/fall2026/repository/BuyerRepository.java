package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
}
