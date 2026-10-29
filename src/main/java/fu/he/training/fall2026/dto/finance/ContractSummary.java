package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.ContractStatus;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * A sales contract with its money position, shared by the payment, settlement and handover screens.
 * {@code clearedAmount} = reconciled payments − processed refunds; {@code outstandingAmount} = sale price − cleared.
 */
@Value
@Builder
public class ContractSummary {
    Long id;
    String code;
    ContractStatus status;
    boolean saleClosed;
    String buyerName;
    String vehicleLabel;
    String vin;
    BigDecimal salePrice;
    BigDecimal depositAmount;
    BigDecimal paidAmount;
    BigDecimal pendingAmount;
    BigDecimal refundedAmount;
    BigDecimal clearedAmount;
    BigDecimal outstandingAmount;

    public boolean isFullyPaid() {
        return outstandingAmount.signum() == 0;
    }
}
