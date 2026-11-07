package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
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

    boolean existsByTypeAndTargetTypeAndTargetIdAndStatus(
            ApprovalType type, String targetType, Long targetId, ApprovalStatus status);

    /** Requests of the target, newest first, with the requester loaded for display. */
    @Query("""
            select a from Approval a
            join fetch a.requestedBy
            where a.type = :type and a.targetType = :targetType and a.targetId = :targetId
            order by a.createdAt desc
            """)
    List<Approval> findRequests(
            @Param("type") ApprovalType type,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    @Query("""
            select a from Approval a
            left join fetch a.approvedBy
            where a.type = :type
              and a.targetType = :targetType
              and a.targetId = :targetId
              and a.status = :status
            order by a.id desc
            """)
    List<Approval> findDecisions(
            @Param("type") ApprovalType type,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("status") ApprovalStatus status);
}
