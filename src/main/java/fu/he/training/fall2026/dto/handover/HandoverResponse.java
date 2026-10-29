package fu.he.training.fall2026.dto.handover;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 05.05 A handover with its checklist items, as shown on the checklist, receipt and close screens. */
@Value
@Builder
public class HandoverResponse {
    Long id;
    String code;
    HandoverStatus status;
    ContractSummary contract;
    String consignorName;
    Integer intakeOdometerKm;
    Integer odometerKm;
    Integer keyCount;
    String accessories;
    String photoPaths;
    String receiverSignaturePath;
    String staffSignaturePath;
    LocalDateTime handedOverAt;
    boolean receiptIssued;
    boolean saleCloseChecked;
    List<HandoverItemForm> items;

    /** Signed handovers are immutable. */
    public boolean isLocked() {
        return status == HandoverStatus.COMPLETED || status == HandoverStatus.CLOSED;
    }
}
