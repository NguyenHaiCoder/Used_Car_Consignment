package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> { }
