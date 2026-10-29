package fu.he.training.fall2026.controller.web.handover;

import fu.he.training.fall2026.dto.handover.HandoverQueueResponse;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.service.HandoverService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** UI-050 Dashboard of the handover role. */
@Controller
@RequiredArgsConstructor
public class HandoverDashboardController {

    private final HandoverService handoverService;

    @GetMapping("/handover/dashboard")
    public String dashboard(Model model) {
        List<HandoverQueueResponse> queue = handoverService.queue();
        model.addAttribute("eligibleCount", queue.stream()
                .filter(q -> q.isEligible() && q.getHandoverStatus() != HandoverStatus.COMPLETED).count());
        model.addAttribute("blockedCount", queue.stream().filter(q -> !q.isEligible()).count());
        model.addAttribute("toClose", queue.stream().filter(q -> q.getHandoverStatus() == HandoverStatus.COMPLETED).toList());
        model.addAttribute("queue", queue);
        return "handover/app-dashboard";
    }
}
