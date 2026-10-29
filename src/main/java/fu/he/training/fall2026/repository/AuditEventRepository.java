package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** History of one record, newest first, with the actor loaded for display. */
    @Query("select e from AuditEvent e left join fetch e.actor where e.entityType = :entityType and e.entityId = :entityId order by e.occurredAt desc")
    List<AuditEvent> findHistory(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}
