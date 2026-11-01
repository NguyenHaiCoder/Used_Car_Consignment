package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    @Query("""
            select a from Approval a
            join fetch a.requestedBy
            left join fetch a.approvedBy
            order by a.createdAt desc
            """)
    List<Approval> findAllFetched();

    @Query("""
            select a from Approval a
            join fetch a.requestedBy
            left join fetch a.approvedBy
            where a.id = :id
            """)
    Optional<Approval> findByIdFetched(@Param("id") Long id);

    long countByStatus(ApprovalStatus status);

    long countByRequestedBy_Id(Long userId);

    long countByApprovedBy_Id(Long userId);
}
