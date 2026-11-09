package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsignmentAgreementRepository extends JpaRepository<ConsignmentAgreement, Long> {

    /** Hợp đồng của hồ sơ (quan hệ 1-1). */
    Optional<ConsignmentAgreement> findByConsignmentId(Long consignmentId);

    /** Mọi hợp đồng kèm hồ sơ + chủ xe, mới nhất trước. */
    @Query("""
            select a from ConsignmentAgreement a
            join fetch a.consignment c join fetch c.consignor
            order by a.id desc
            """)
    List<ConsignmentAgreement> findAllWithConsignment();

    @Query("select a from ConsignmentAgreement a join fetch a.consignment where a.id = :id")
    Optional<ConsignmentAgreement> findByIdWithConsignment(@Param("id") Long id);
}
