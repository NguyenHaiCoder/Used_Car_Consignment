package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsignmentRepository extends JpaRepository<Consignment, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern such as {@code %camry%}; use {@code %} to match all. */
    @Query(value = """
            select c from Consignment c join fetch c.consignor o join fetch c.vehicle v
            where (:status is null or c.status = :status)
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """,
            countQuery = """
            select count(c) from Consignment c join c.consignor o join c.vehicle v
            where (:status is null or c.status = :status)
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """)
    Page<Consignment> search(
            @Param("pattern") String pattern,
            @Param("status") ConsignmentStatus status,
            Pageable pageable);

    /** Tất cả hồ sơ kèm chủ xe + xe, mới nhất trước. */
    @Query("select c from Consignment c join fetch c.consignor join fetch c.vehicle order by c.createdAt desc")
    List<Consignment> findAllWithDetail();

    @Query("select c from Consignment c join fetch c.consignor join fetch c.vehicle where c.id = :id")
    Optional<Consignment> findDetailById(@Param("id") Long id);

    /** Nguyễn Vũ Trường An: hàng đợi kiểm định — {@code pattern} như {@link #search}, chỉ lấy các trạng thái truyền vào. */
    @Query(value = """
            select c from Consignment c join fetch c.consignor o join fetch c.vehicle v
            where c.status in :statuses
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """,
            countQuery = """
            select count(c) from Consignment c join c.consignor o join c.vehicle v
            where c.status in :statuses
              and (lower(c.code) like :pattern
                   or lower(o.fullName) like :pattern
                   or lower(v.vin) like :pattern
                   or lower(v.make) like :pattern
                   or lower(v.model) like :pattern)
            """)
    Page<Consignment> searchByStatuses(
            @Param("pattern") String pattern,
            @Param("statuses") List<ConsignmentStatus> statuses,
            Pageable pageable);

    /** Nguyễn Vũ Trường An: hồ sơ theo trạng thái kèm chủ xe + xe, mới nhất trước (ô chọn xe khi đăng tin). */
    @Query("select c from Consignment c join fetch c.consignor join fetch c.vehicle where c.status = :status order by c.id desc")
    List<Consignment> findAllByStatusWithVehicle(@Param("status") ConsignmentStatus status);

    /** Codes of the vehicle's reservations in the given status (withdraw blockers). */
    @Query("select r.code from Reservation r where r.vehicle.id = :vehicleId and r.status = :status order by r.code")
    List<String> findReservationCodes(@Param("vehicleId") Long vehicleId, @Param("status") ReservationStatus status);

    /** Deposit money still held on the vehicle's reservations in the given status. */
    @Query("select coalesce(sum(r.depositAmount), 0) from Reservation r where r.vehicle.id = :vehicleId and r.status = :status")
    BigDecimal sumReservationDeposits(@Param("vehicleId") Long vehicleId, @Param("status") ReservationStatus status);

    /** Hà Lệ Quyên: hợp đồng bán của xe, trừ trạng thái truyền vào. Hợp đồng nháp vẫn tính vì phiếu cọc đã rời ACTIVE. */
    @Query("select s.code from SalesContract s where s.reservation.vehicle.id = :vehicleId and s.status <> :excluded order by s.code")
    List<String> findContractCodes(@Param("vehicleId") Long vehicleId, @Param("excluded") ContractStatus excluded);

    /** Rows of {status, count} used for the status counters above the list. */
    @Query("select c.status, count(c) from Consignment c group by c.status")
    List<Object[]> countByStatus();

    @Query("""
            select c from Consignment c join fetch c.consignor
            where c.vehicle.id = :vehicleId
            order by c.id desc
            """)
    List<Consignment> findByVehicleIdOrderByIdDesc(@Param("vehicleId") Long vehicleId);
}
