package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    boolean existsByTypeAndTargetTypeAndTargetIdAndStatus(
            ApprovalType type, String targetType, Long targetId, ApprovalStatus status);

    /** Requests of the target, newest first, with the requester loaded for display. */
    @Query("select a from Approval a join fetch a.requestedBy where a.type = :type and a.targetType = :targetType and a.targetId = :targetId order by a.createdAt desc")
    List<Approval> findRequests(
            @Param("type") ApprovalType type, @Param("targetType") String targetType, @Param("targetId") Long targetId);
}
