package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.service.ConsignmentAgreementService;
import fu.he.training.fall2026.service.ConsignmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Manager view of consignments: read-only list, detail and the withdraw request screen (02.05). */
@Controller
@RequestMapping("/manager/consignments")
@RequiredArgsConstructor
public class ManagerConsignmentWebController {

    private static final int PAGE_SIZE = 10;

    private final ConsignmentService consignmentService;
    private final ConsignmentAgreementService agreementService;

    /** The shared sidebar reads this list for roles without their own menu; the manager has none here. */
    @ModelAttribute("navigation")
    public List<Object> navigation() {
        return List.of();
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ConsignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ConsignmentResponse> result = consignmentService.search(q, status, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        model.addAttribute("counts", consignmentService.countByStatus());
        return "manager/consignments/app-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("agreement", agreementService.findLatest(id).orElse(null));
        model.addAttribute("withdrawRequest", consignmentService.findWithdrawRequest(id).orElse(null));
        return "manager/consignments/app-detail";
    }

    @GetMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, Model model) {
        var checks = consignmentService.withdrawChecks(id);
        model.addAttribute("consignment", consignmentService.get(id));
        // the last check is "no pending request", which only matters to the requester
        model.addAttribute("checks", checks.subList(0, checks.size() - 1));
        model.addAttribute("withdrawRequest", consignmentService.findWithdrawRequest(id).orElse(null));
        return "manager/consignments/app-withdraw";
    }
}
