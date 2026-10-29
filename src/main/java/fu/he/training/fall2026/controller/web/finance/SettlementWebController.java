package fu.he.training.fall2026.controller.web.finance;

import static fu.he.training.fall2026.controller.web.finance.PaymentWebController.run;

import fu.he.training.fall2026.dto.finance.SettlementAdjustRequest;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SettlementService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

/** 05.03 Commission & Consignor Settlement (Finance side). */
@Controller
@RequestMapping("/finance/settlements")
@RequiredArgsConstructor
public class SettlementWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/finance/settlements";
    private static final String DETAIL_VIEW = "finance/settlements/app-detail";

    private final SettlementService settlementService;

    @GetMapping
    public String list(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("page", settlementService.search(status, page, PAGE_SIZE));
        model.addAttribute("status", status);
        model.addAttribute("counts", settlementService.countByStatus());
        model.addAttribute("readyContracts", settlementService.contractsWithoutSettlement());
        return "finance/settlements/app-list";
    }

    @PostMapping
    public String create(@RequestParam Long contractId, HttpSession session, RedirectAttributes flash) {
        try {
            Long id = settlementService.create(contractId, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã lập quyết toán nháp theo agreement ACTIVE.");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE;
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("form", settlementService.formFor(id));
        addDetail(id, model);
        return DETAIL_VIEW;
    }

    @PostMapping("/adjust")
    public String adjust(
            @Valid @ModelAttribute("form") SettlementAdjustRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        if (!errors.hasErrors()) {
            try {
                settlementService.adjust(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã lưu và tính lại quyết toán.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addDetail(form.getId(), model);
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã gửi quyết toán cho Manager duyệt.",
                () -> settlementService.submit(id, SessionAuthUtils.getUserId(session)));
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/payout")
    public String createPayout(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        try {
            Long payoutId = settlementService.createPayout(id, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã lập lệnh chi cho chủ xe.");
            return "redirect:/finance/payouts/" + payoutId;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/" + id;
        }
    }

    private void addDetail(Long id, Model model) {
        model.addAttribute("settlement", settlementService.get(id));
        model.addAttribute("contract", settlementService.contractOf(id));
        model.addAttribute("checks", settlementService.checks(id));
        model.addAttribute("payouts", settlementService.payouts(id));
        model.addAttribute("history", settlementService.history(id));
    }
}
