package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.PayoutRequest;
import fu.he.training.fall2026.dto.finance.SettlementFilter;
import fu.he.training.fall2026.dto.finance.SettlementView;
import fu.he.training.fall2026.entity.sales.SalesContract;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** Vũ Mạnh Cường: quyết toán và chi trả chủ xe. */
public interface SettlementService {

    Map<String, Map<String, String>> statusMeta();

    Map<String, Map<String, String>> reviewStatusMeta();

    Page<SettlementView> search(SettlementFilter filter, boolean excludeDraft, int page, Integer size);

    Map<String, Long> countByStatus(boolean excludeDraft);

    List<String> listStaffNames();

    SettlementView get(Long id);

    SettlementView getForReview(Long id);

    List<Map<String, String>> steps(Long id);

    void openFor(SalesContract contract);

    void submit(Long id);

    void decide(Long id, String decision, String reason, Long actorUserId);

    void payout(Long id, PayoutRequest form);

    List<Map<String, Object>> approvalQueueRows();
}
