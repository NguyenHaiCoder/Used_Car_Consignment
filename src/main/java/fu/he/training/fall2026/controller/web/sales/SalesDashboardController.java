package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.auth.AuthSessionResponse;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.service.AuthService;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.SalesPipelineViewService;
import fu.he.training.fall2026.service.SalesSchedulingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SalesDashboardController {
    private final AuthService authService;
    private final InquiryService inquiryService;
    private final SalesSchedulingService schedulingService;
    private final SalesPipelineViewService pipelineViewService;

    @GetMapping("/sales/dashboard")
    public String dashboard(HttpSession session, Model model) {
        AuthSessionResponse auth = authService.currentSession(session);
        if (auth == null) {
            return "redirect:/?loginRequired=1";
        }

        model.addAttribute("openInquiryCount",
                inquiryService.list(null, InquiryStatus.OPEN, null, 1, 1).getTotalElements());
        model.addAttribute("scheduledViewingCount",
                schedulingService.viewings(null, "SCHEDULED", null, 1, 1).getTotalElements());
        model.addAttribute("activeReservationCount",
                pipelineViewService.reservations(ReservationStatus.ACTIVE, 1).getTotalElements());
        model.addAttribute("recentInquiries",
                inquiryService.list(null, null, null, 1, 5).getContent());
        return "sales/dashboard";
    }
}
