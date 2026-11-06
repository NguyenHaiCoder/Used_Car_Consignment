package fu.he.training.fall2026.repository;
import fu.he.training.fall2026.entity.payment.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByReservationId(Long reservationId);
}
