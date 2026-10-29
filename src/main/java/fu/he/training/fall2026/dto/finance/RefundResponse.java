package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefundResponse {
    Long id;
    String code;
    Long paymentId;
    String reservationCode;
    BigDecimal amount;
    RefundStatus status;
    String reason;
    String evidencePath;
    LocalDateTime processedAt;
    LocalDateTime createdAt;
}
