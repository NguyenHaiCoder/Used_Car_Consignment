package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.dto.handover.HandoverQueueResponse;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.service.HandoverService;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.service.PayoutService;
import fu.he.training.fall2026.service.SettlementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UI-049 Dashboard of the finance role. */
@Controller
@RequiredArgsConstructor
public class FinanceDashboardController {

    private final PaymentService paymentService;
    private final SettlementService settlementService;
    private final PayoutService payoutService;
    private final HandoverService handoverService;

    @GetMapping("/finance/dashboard")
    public String dashboard(Model model) {
        var payments = paymentService.countByStatus();
        var settlements = settlementService.countByStatus();
        List<HandoverQueueResponse> queue = handoverService.queue();
        model.addAttribute("pendingPayments", payments.get(PaymentStatus.PENDING) + payments.get(PaymentStatus.PROCESSING));
        model.addAttribute("pendingRefunds", paymentService.pendingRefundCount());
        model.addAttribute("draftSettlements", settlements.get(SettlementStatus.DRAFT) + settlements.get(SettlementStatus.REJECTED));
        model.addAttribute("reviewSettlements", settlements.get(SettlementStatus.PENDING_REVIEW));
        model.addAttribute("openPayouts", payoutService.openCount());
        model.addAttribute("unpaidContracts", queue.stream().filter(q -> !q.getContract().isFullyPaid()).toList());
        model.addAttribute("readyToSettle", settlementService.contractsWithoutSettlement());
        model.addAttribute("recentPayments", paymentService.search(null, null, 0, 5).getContent());
        return "finance/app-dashboard";
    }
}
