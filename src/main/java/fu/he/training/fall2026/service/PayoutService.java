package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.HistoryEntry;
import fu.he.training.fall2026.dto.finance.PayoutCompleteRequest;
import fu.he.training.fall2026.dto.finance.PayoutResponse;
import fu.he.training.fall2026.entity.enums.PayoutStatus;
import java.util.List;
import org.springframework.data.domain.Page;

/** Owner payouts created from approved settlements (UI-047 / UI-048). No bank call is made. */
public interface PayoutService {

    Page<PayoutResponse> search(PayoutStatus status, int page, int size);

    PayoutResponse get(Long id);

    /** PENDING → COMPLETED with the bank reference and evidence; the settlement becomes PAID_OUT. */
    void complete(PayoutCompleteRequest form, Long actorUserId);

    /** PENDING → FAILED with a reason; a new payout can then be created from the settlement. */
    void fail(Long id, String reason, Long actorUserId);

    long openCount();

    List<HistoryEntry> history(Long id);
}
