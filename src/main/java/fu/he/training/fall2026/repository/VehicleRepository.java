package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
