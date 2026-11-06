package fu.he.training.fall2026.dto.sales;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationHistoryResponse {
    private Instant occurredAt;
    private String action;
    private String actorName;
    private String detail;
}
