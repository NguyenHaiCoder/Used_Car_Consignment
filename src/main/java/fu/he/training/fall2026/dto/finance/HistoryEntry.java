package fu.he.training.fall2026.dto.finance;

import java.time.LocalDateTime;
import lombok.Value;

/** One audit line in the history section of a detail screen. */
@Value
public class HistoryEntry {
    LocalDateTime time;
    String actorName;
    String action;
    String detail;
}
