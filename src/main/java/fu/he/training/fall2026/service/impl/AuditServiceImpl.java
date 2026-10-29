package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.entity.auth.AuditEvent;
import fu.he.training.fall2026.repository.AuditEventRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.AuditService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int DETAIL_MAX = 1000;

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(Long actorUserId, String action, String entityType, Long entityId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setActor(actorUserId == null ? null : userRepository.getReferenceById(actorUserId));
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetail(detail == null || detail.length() <= DETAIL_MAX ? detail : detail.substring(0, DETAIL_MAX));
        event.setOccurredAt(Instant.now());
        auditEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(String entityType, Long entityId) {
        return auditEventRepository.findHistory(entityType, entityId).stream()
                .map(e -> new HistoryEntry(
                        LocalDateTime.ofInstant(e.getOccurredAt(), ZoneId.systemDefault()),
                        e.getActor() != null ? e.getActor().getFullName() : "Hệ thống",
                        e.getAction(),
                        e.getDetail()))
                .toList();
    }
}
