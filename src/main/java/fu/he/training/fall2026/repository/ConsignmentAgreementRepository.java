package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsignmentAgreementRepository extends JpaRepository<ConsignmentAgreement, Long> {

    /** The newest version of the consignment's agreement. */
    Optional<ConsignmentAgreement> findFirstByConsignmentIdOrderByVersionNoDesc(Long consignmentId);

    List<ConsignmentAgreement> findByConsignmentIdOrderByVersionNoDesc(Long consignmentId);

    @EntityGraph(attributePaths = "consignment")
    @Query(value = """
            select a from ConsignmentAgreement a
            where (:status is null or a.status = :status)
              and (lower(a.code) like :pattern
                   or lower(a.consignment.code) like :pattern)
            """,
            countQuery = """
            select count(a) from ConsignmentAgreement a
            where (:status is null or a.status = :status)
              and (lower(a.code) like :pattern
                   or lower(a.consignment.code) like :pattern)
            """)
    Page<ConsignmentAgreement> search(
            @Param("pattern") String pattern,
            @Param("status") AgreementStatus status,
            Pageable pageable);

    @Query("select a from ConsignmentAgreement a join fetch a.consignment where a.id = :id")
    Optional<ConsignmentAgreement> findByIdWithConsignment(@Param("id") Long id);

    @Query("select a.status, count(a) from ConsignmentAgreement a group by a.status")
    List<Object[]> countByStatus();
}
