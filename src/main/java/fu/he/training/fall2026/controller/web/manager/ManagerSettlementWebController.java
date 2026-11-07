package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.DealFilterSupport;
import fu.he.training.fall2026.dto.finance.SettlementFilter;
import fu.he.training.fall2026.dto.finance.SettlementView;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SettlementService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vũ Mạnh Cường: Quản lý duyệt quyết toán — Controller → SettlementService → DB.
 */
@Controller
@RequestMapping("/manager/settlements")
@RequiredArgsConstructor
public class ManagerSettlementWebController {

    private static final String BASE = "/manager/settlements";
    private static final String LIST_VIEW = "manager/settlements/list";
    private static final String DETAIL_VIEW = "manager/settlements/detail";

    private final SettlementService settlementService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", settlementService.reviewStatusMeta());
    }

    @GetMapping
    public String list(
            @ModelAttribute("filter") SettlementFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<SettlementView> result = CatalogPageSupport.lastPageIfBeyond(
                settlementService.search(filter, true, page, pageSize),
                last -> settlementService.search(filter, true, last, pageSize));
        DealFilterSupport.bindListModel(model, BASE, filter, pageSize);
        model.addAttribute("page", result);
        model.addAttribute("counts", settlementService.countByStatus(true));
        model.addAttribute("staffs", settlementService.listStaffNames());
        return LIST_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("item", settlementService.getForReview(id));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/decide")
    public String decide(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String decision,
            @RequestParam(defaultValue = "") String reason,
            HttpSession session,
            RedirectAttributes flash) {
        try {
            settlementService.decide(id, decision, reason, SessionAuthUtils.getUserId(session));
            if ("APPROVED".equals(decision)) {
                flash.addFlashAttribute("flashMessage", "Đã duyệt quyết toán.");
                return "redirect:" + BASE + "/" + id;
            }
            flash.addFlashAttribute("flashMessage", "Đã trả quyết toán về cho Tài chính.");
            return "redirect:" + BASE;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/" + id;
        }
    }
}
