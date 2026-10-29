package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/** A buyer payment as shown on the list and detail screens. */
@Value
@Builder
public class PaymentResponse {
    Long id;
    String code;
    Long contractId;
    String contractCode;
    String buyerName;
    BigDecimal amount;
    PaymentStatus status;
    String payerName;
    String referenceCode;
    String method;
    LocalDateTime paidAt;
    String evidencePath;
    LocalDateTime createdAt;
    /** Provider transaction (payOS) linked to this payment, null when recorded manually. */
    String providerTxnId;
    PaymentStatus transactionStatus;
}
