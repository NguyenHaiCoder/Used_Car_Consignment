package fu.he.training.fall2026.dto.consignment;

import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/** The withdraw request sent to the manager: its approval status plus what the staff entered. */
@Value
@Builder
public class WithdrawRequestResponse {

    ApprovalStatus status;
    String reason;
    String requestedByName;
    LocalDateTime createdAt;
    LocalDate returnDate;
    String condition;
    String evidencePath;
}
