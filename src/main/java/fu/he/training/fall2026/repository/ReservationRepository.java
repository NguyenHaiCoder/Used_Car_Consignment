package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
