package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.HistoryEntry;
import java.util.List;

/** Append-only history of business actions (who did what, when, on which record). */
public interface AuditService {

    void record(Long actorUserId, String action, String entityType, Long entityId, String detail);

    /** History of one record, newest first. */
    List<HistoryEntry> history(String entityType, Long entityId);
}
