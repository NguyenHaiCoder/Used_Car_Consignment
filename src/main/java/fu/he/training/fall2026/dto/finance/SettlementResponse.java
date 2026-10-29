package fu.he.training.fall2026.dto.finance;

import fu.he.training.fall2026.entity.enums.SettlementStatus;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * A settlement with every term of its formula:
 * net payout = base − commission − fees − approved costs − refunds posted + forfeited deposit kept for the owner.
 */
@Value
@Builder
public class SettlementResponse {
    Long id;
    String code;
    SettlementStatus status;
    Long contractId;
    String contractCode;
    String vehicleLabel;
    Long consignmentId;
    String consignmentCode;
    String consignorName;
    String agreementCode;
    int agreementVersion;
    BigDecimal commissionRate;
    BigDecimal fixedFee;
    BigDecimal priceFloor;
    String payoutBank;
    String payoutAccountName;
    String payoutAccountMasked;
    BigDecimal baseAmount;
    BigDecimal commissionAmount;
    BigDecimal feeAmount;
    BigDecimal approvedCostAmount;
    BigDecimal refundHeldAmount;
    BigDecimal depositHeldAmount;
    BigDecimal netPayoutAmount;
    String notes;

    public boolean isEditable() {
        return status == SettlementStatus.DRAFT || status == SettlementStatus.REJECTED;
    }
}
