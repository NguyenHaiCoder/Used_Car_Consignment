package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select e from AuditEvent e
            left join fetch e.actor
            order by e.occurredAt desc
            """)
    List<AuditEvent> findAllWithActorOrderByOccurredAtDesc();

    @Modifying(clearAutomatically = true)
    @Query("update AuditEvent e set e.actor = null where e.actor.id = :userId")
    void clearActorByUserId(@Param("userId") Long userId);
}
