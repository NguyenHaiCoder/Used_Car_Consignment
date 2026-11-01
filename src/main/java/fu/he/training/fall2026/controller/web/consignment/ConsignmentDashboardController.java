package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.service.ConsignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UI-041 Dashboard of the consignment role. */
@Controller
@RequiredArgsConstructor
public class ConsignmentDashboardController {

    private final ConsignmentService consignmentService;

    @GetMapping("/consignment/dashboard")
    public String dashboard(Model model) {
        var counts = consignmentService.countByStatus();
        model.addAttribute("draftCount", counts.get(ConsignmentStatus.DRAFT));
        model.addAttribute("submittedCount", counts.get(ConsignmentStatus.SUBMITTED));
        model.addAttribute("withdrawnCount", counts.get(ConsignmentStatus.WITHDRAWN));
        model.addAttribute("recent", consignmentService.search(null, null, 0, 5).getContent());
        return "consignment/app-dashboard";
    }
}
