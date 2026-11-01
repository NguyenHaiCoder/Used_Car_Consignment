package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.repository.AuditEventRepository;
import fu.he.training.fall2026.service.AuditService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;

    @Override
    @Transactional
    public void record(User actor, String action, String entityType, Long entityId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetail(detail);
        event.setOccurredAt(Instant.now());
        auditEventRepository.save(event);
    }
}
