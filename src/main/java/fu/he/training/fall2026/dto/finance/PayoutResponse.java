package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.PayoutStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PayoutResponse {
    Long id;
    String code;
    Long settlementId;
    String settlementCode;
    String contractCode;
    BigDecimal amount;
    PayoutStatus status;
    String recipientName;
    String bankName;
    String accountNumberMasked;
    String referenceCode;
    String evidencePath;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
}
