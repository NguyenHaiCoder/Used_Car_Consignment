package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.service.HandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 05.04 Handover queue, read-only Finance view to follow which contracts still block on money. */
@Controller
@RequiredArgsConstructor
public class FinanceHandoverWebController {

    private final HandoverService handoverService;

    @GetMapping("/finance/handovers")
    public String queue(Model model) {
        model.addAttribute("queue", handoverService.queue());
        return "finance/handovers/app-list";
    }
}
