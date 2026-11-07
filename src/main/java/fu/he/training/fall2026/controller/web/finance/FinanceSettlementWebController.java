package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.DealFilterSupport;
import fu.he.training.fall2026.dto.finance.PayoutRequest;
import fu.he.training.fall2026.dto.finance.SettlementFilter;
import fu.he.training.fall2026.dto.finance.SettlementView;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vũ Mạnh Cường: Quyết toán và Chi trả chủ xe — Controller → SettlementService → DB.
 */
@Controller
@RequestMapping("/finance/settlements")
@RequiredArgsConstructor
public class FinanceSettlementWebController {

    private static final String BASE = "/finance/settlements";
    private static final String LIST_VIEW = "finance/settlements/list";
    private static final String DETAIL_VIEW = "finance/settlements/detail";

    private final SettlementService settlementService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", settlementService.statusMeta());
    }

    @GetMapping
    public String list(
            @ModelAttribute("filter") SettlementFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<SettlementView> result = CatalogPageSupport.lastPageIfBeyond(
                settlementService.search(filter, false, page, pageSize),
                last -> settlementService.search(filter, false, last, pageSize));
        DealFilterSupport.bindListModel(model, BASE, filter, pageSize);
        model.addAttribute("page", result);
        model.addAttribute("counts", settlementService.countByStatus(false));
        model.addAttribute("staffs", settlementService.listStaffNames());
        return LIST_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        SettlementView item = settlementService.get(id);
        model.addAttribute("item", item);
        model.addAttribute("steps", settlementService.steps(id));
        model.addAttribute("form", new PayoutRequest());
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable long id, RedirectAttributes flash) {
        settlementService.submit(id);
        flash.addFlashAttribute("flashMessage", "Đã gửi Quản lý duyệt quyết toán.");
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/payout")
    public String payout(
            @PathVariable long id,
            @Valid @ModelAttribute("form") PayoutRequest form,
            BindingResult errors,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("flashError", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:" + BASE + "/" + id;
        }
        try {
            settlementService.payout(id, form);
            SettlementView item = settlementService.get(id);
            flash.addFlashAttribute("flashMessage",
                    "Đã chi trả chủ xe, hồ sơ ký gửi " + item.getFile() + " hoàn thành.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }
}
