package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.RuleCheck;
import fu.he.training.fall2026.dto.handover.HandoverChecklistRequest;
import fu.he.training.fall2026.dto.handover.HandoverQueueResponse;
import fu.he.training.fall2026.dto.handover.HandoverResponse;
import java.util.List;

/** 05.04 Vehicle Handover Queue and 05.05 Handover Checklist, Receipt and Close sale. */
public interface HandoverService {

    /** Signed contracts that are not closed yet, each with its handover blockers (BR-08). */
    List<HandoverQueueResponse> queue();

    /** Opens the handover record of a contract (status READY or BLOCKED) with the default checklist items. */
    Long open(Long contractId, Long actorUserId);

    HandoverResponse get(Long id);

    HandoverChecklistRequest formFor(Long id);

    /** Blockers plus the checklist requirements that must pass before the handover can be completed. */
    List<RuleCheck> completionChecks(Long id);

    /** Saves the checklist as a draft; a completed handover is immutable. */
    void saveChecklist(HandoverChecklistRequest form, Long actorUserId);

    /**
     * Signs the handover: COMPLETED, receipt issued, listing SOLD and consignment SOLD.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException while any completion check fails
     */
    void complete(Long id, Long actorUserId);

    /** The three separate milestones of closing a sale: payment, ownership transfer and physical handover. */
    List<RuleCheck> closeChecks(Long id);

    /** Closes the sale: handover CLOSED, sales contract CLOSED. */
    void closeSale(Long id, String note, Long actorUserId);

    List<HistoryEntry> history(Long id);
}
