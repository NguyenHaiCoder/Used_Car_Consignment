package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.payment.Refund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByReservationId(Long reservationId);

    boolean existsByReservationId(Long reservationId);

    boolean existsByReferenceCodeIgnoreCase(String referenceCode);

    @Query("""
            select r from Refund r
              join fetch r.reservation res
              join fetch res.buyer
              join fetch res.vehicle
              left join fetch res.assignedTo
            where r.reservation.id = :reservationId
            """)
    Optional<Refund> findDetailByReservationId(@Param("reservationId") Long reservationId);

    @Query("""
            select r from Refund r
              join fetch r.reservation res
              join fetch res.buyer
              join fetch res.vehicle
              left join fetch res.assignedTo
            where r.status = fu.he.training.fall2026.entity.enums.RefundStatus.PENDING
               or r.status = fu.he.training.fall2026.entity.enums.RefundStatus.PROCESSED
            order by r.id desc
            """)
    List<Refund> findAllFetched();
}
