package fu.he.training.fall2026.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Old dashboard links of the roles whose work starts on a list screen.
 * Admin → {@code AdminWebController}, Manager → {@code ManagerWebController},
 * Consignment → {@code ConsignmentDashboardController},
 * Inspector → {@code InspectorVehiclesWebController},
 * Sales → {@code SalesInquiryWebController}, Documents → {@code TransferWebController},
 * Finance → {@code FinancePaymentWebController}, Handover → {@code HandoverWebController}.
 */
@Controller
public class RoleDashboardController {

    @GetMapping("/sales/dashboard")
    public String salesDashboard() {
        return "redirect:/sales/inquiries";
    }

    @GetMapping("/finance/dashboard")
    public String financeDashboard() {
        return "redirect:/finance/payments";
    }

    @GetMapping("/handover/dashboard")
    public String handoverDashboard() {
        return "redirect:/handover/handovers";
    }

    @GetMapping("/documents/dashboard")
    public String documentsDashboard() {
        return "redirect:/documents/transfers";
    }
}
