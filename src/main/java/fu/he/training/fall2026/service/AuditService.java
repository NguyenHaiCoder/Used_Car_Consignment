package fu.he.training.fall2026.service;

import fu.he.training.fall2026.entity.auth.User;

public interface AuditService {

    void record(User actor, String action, String entityType, Long entityId, String detail);
}
