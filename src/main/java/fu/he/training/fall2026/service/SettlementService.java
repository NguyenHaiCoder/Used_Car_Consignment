package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PayoutResponse;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.dto.finance.SettlementAdjustRequest;
import fu.he.training.fall2026.dto.finance.SettlementDecisionRequest;
import fu.he.training.fall2026.dto.finance.SettlementResponse;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** 05.03 Commission & Consignor Settlement (Finance prepares, Manager reviews, Finance pays out). */
public interface SettlementService {

    Page<SettlementResponse> search(SettlementStatus status, int page, int size);

    Map<SettlementStatus, Long> countByStatus();

    /** Signed contracts that have no settlement yet. */
    List<ContractSummary> contractsWithoutSettlement();

    /**
     * Creates a DRAFT settlement from the ACTIVE agreement of the consignment sold under the contract.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when the contract already has a settlement
     *         or the consignment has no ACTIVE agreement
     */
    Long create(Long contractId, Long actorUserId);

    SettlementResponse get(Long id);

    ContractSummary contractOf(Long id);

    /** Conditions to pass before the settlement can be sent to the Manager. */
    List<RuleCheck> checks(Long id);

    SettlementAdjustRequest formFor(Long id);

    /** Saves Finance-entered fees/costs/forfeited deposit and recalculates; only DRAFT or REJECTED. */
    void adjust(SettlementAdjustRequest form, Long actorUserId);

    /** DRAFT/REJECTED → PENDING_REVIEW and a PENDING SETTLEMENT approval for the Manager. */
    void submit(Long id, Long actorUserId);

    /** Manager decision; NEED_MORE_INFO sends the settlement back to DRAFT. */
    void decide(SettlementDecisionRequest form, Long actorUserId);

    List<PayoutResponse> payouts(Long id);

    /**
     * Creates the owner payout order for an APPROVED settlement.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException unless the contract is fully paid with cleared
     *         funds and no other payout is open
     */
    Long createPayout(Long id, Long actorUserId);

    List<HistoryEntry> history(Long id);
}
