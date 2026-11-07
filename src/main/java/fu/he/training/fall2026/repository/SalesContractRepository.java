package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesContractRepository extends JpaRepository<SalesContract, Long> {

    @Query("""
            select sc from SalesContract sc
            join fetch sc.reservation r
            join fetch r.buyer
            join fetch r.vehicle
            join fetch r.inquiry i
            left join fetch i.assignedTo
            left join fetch r.assignedTo
            where sc.id = :id
            """)
    Optional<SalesContract> findDetailById(@Param("id") Long id);

    /**
     * Hợp đồng đã ký, chưa có TransferCase — ứng viên tạo hồ sơ sang tên.
     * Điều kiện chi tiết (cọc / inquiry / ký gửi) lọc tiếp ở Service.
     */
    @Query("""
            select sc from SalesContract sc
            join fetch sc.reservation r
            join fetch r.buyer
            join fetch r.vehicle
            join fetch r.inquiry i
            left join fetch i.assignedTo
            left join fetch r.assignedTo
            where sc.signedOn is not null
              and sc.status <> :cancelled
              and not exists (select 1 from TransferCase t where t.salesContract = sc)
            order by sc.id desc
            """)
    List<SalesContract> findSignedWithoutTransfer(@Param("cancelled") ContractStatus cancelled);
}
