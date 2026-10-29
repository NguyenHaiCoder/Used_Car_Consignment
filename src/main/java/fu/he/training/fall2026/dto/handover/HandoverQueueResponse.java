package fu.he.training.fall2026.dto.handover;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 05.04 One contract in the handover queue with its blockers (BR-08). */
@Value
@Builder
public class HandoverQueueResponse {
    ContractSummary contract;
    /** Null until handover staff opens a handover for the contract. */
    Long handoverId;
    String handoverCode;
    HandoverStatus handoverStatus;
    TransferStatus transferStatus;
    List<RuleCheck> checks;

    public boolean isEligible() {
        return checks.stream().allMatch(RuleCheck::isPassed);
    }

    public List<RuleCheck> getBlockers() {
        return checks.stream().filter(check -> !check.isPassed()).toList();
    }
}
