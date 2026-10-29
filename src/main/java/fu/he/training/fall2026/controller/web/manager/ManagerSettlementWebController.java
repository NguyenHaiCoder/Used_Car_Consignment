package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.dto.finance.SettlementDecisionRequest;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SettlementService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/** 05.03 Manager review of settlements: read-only figures plus approve / reject / ask for more info. */
@Controller
@RequestMapping("/manager/settlements")
@RequiredArgsConstructor
public class ManagerSettlementWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/manager/settlements";
    private static final String DETAIL_VIEW = "manager/settlements/app-detail";

    private final SettlementService settlementService;

    /** The shared sidebar reads this list for roles without their own menu; the manager has none here. */
    @ModelAttribute("navigation")
    public List<Object> navigation() {
        return List.of();
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("page", settlementService.search(status, page, PAGE_SIZE));
        model.addAttribute("status", status);
        model.addAttribute("counts", settlementService.countByStatus());
        return "manager/settlements/app-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SettlementDecisionRequest form = new SettlementDecisionRequest();
        form.setId(id);
        model.addAttribute("form", form);
        addDetail(id, model);
        return DETAIL_VIEW;
    }

    @PostMapping("/decision")
    public String decide(
            @Valid @ModelAttribute("form") SettlementDecisionRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        if (!errors.hasErrors()) {
            try {
                settlementService.decide(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã ghi nhận quyết định " + form.getDecision() + ".");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addDetail(form.getId(), model);
        return DETAIL_VIEW;
    }

    private void addDetail(Long id, Model model) {
        model.addAttribute("settlement", settlementService.get(id));
        model.addAttribute("contract", settlementService.contractOf(id));
        model.addAttribute("checks", settlementService.checks(id));
        model.addAttribute("history", settlementService.history(id));
    }
}
