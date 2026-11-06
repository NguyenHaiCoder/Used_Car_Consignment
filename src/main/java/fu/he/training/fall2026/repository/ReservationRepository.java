package fu.he.training.fall2026.repository;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
public interface ReservationRepository extends JpaRepository<Reservation, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Reservation> {
    boolean existsByBuyer_IdAndStatus(Long buyerId, fu.he.training.fall2026.entity.enums.ReservationStatus status);
    boolean existsByVehicleIdAndStatus(Long vehicleId, fu.he.training.fall2026.entity.enums.ReservationStatus status);
    @Query("select distinct v from Reservation r join r.vehicle v order by v.make, v.model")
    java.util.List<Vehicle> findReservedVehicles();
    org.springframework.data.domain.Page<Reservation> findByStatus(fu.he.training.fall2026.entity.enums.ReservationStatus status, org.springframework.data.domain.Pageable pageable);
    java.util.List<Reservation> findByStatus(fu.he.training.fall2026.entity.enums.ReservationStatus status, org.springframework.data.domain.Sort sort);
    @Query("select r.vehicle.id from Reservation r where r.id = :id")
    java.util.Optional<Long> findVehicleId(Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.vehicle.id = :vehicleId and r.status = fu.he.training.fall2026.entity.enums.ReservationStatus.ACTIVE")
    java.util.List<Reservation> lockActiveByVehicleId(Long vehicleId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    java.util.Optional<Reservation> lockById(Long id);
    @Query("""
        select r from Reservation r where r.vehicle.id = :vehicleId
        and r.status = fu.he.training.fall2026.entity.enums.ReservationStatus.ACTIVE
        and (r.holdUntil is null or r.holdUntil >= :today)
        """)
    java.util.List<Reservation> findCurrentHolds(Long vehicleId, LocalDate today);
}
