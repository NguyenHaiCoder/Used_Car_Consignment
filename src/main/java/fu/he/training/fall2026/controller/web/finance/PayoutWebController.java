package fu.he.training.fall2026.controller.web.finance;

import static fu.he.training.fall2026.controller.web.finance.PaymentWebController.run;

import fu.he.training.fall2026.dto.finance.PayoutCompleteRequest;
import fu.he.training.fall2026.entity.enums.PayoutStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.PayoutService;
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

/** UI-047 Owner payouts list and UI-048 payout order detail. */
@Controller
@RequestMapping("/finance/payouts")
@RequiredArgsConstructor
public class PayoutWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/finance/payouts";
    private static final String DETAIL_VIEW = "finance/payouts/app-detail";

    private final PayoutService payoutService;

    @GetMapping
    public String list(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("page", payoutService.search(status, page, PAGE_SIZE));
        model.addAttribute("status", status);
        model.addAttribute("statuses", PayoutStatus.values());
        return "finance/payouts/app-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PayoutCompleteRequest form = new PayoutCompleteRequest();
        form.setId(id);
        model.addAttribute("form", form);
        addDetail(id, model);
        return DETAIL_VIEW;
    }

    @PostMapping("/complete")
    public String complete(
            @Valid @ModelAttribute("form") PayoutCompleteRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        if (!errors.hasErrors()) {
            try {
                payoutService.complete(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã xác nhận chi trả; quyết toán chuyển PAID_OUT.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addDetail(form.getId(), model);
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}/fail")
    public String fail(@PathVariable Long id, @RequestParam(required = false) String reason,
                       HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã đánh dấu lệnh chi thất bại.",
                () -> payoutService.fail(id, reason, SessionAuthUtils.getUserId(session)));
        return "redirect:" + BASE + "/" + id;
    }

    private void addDetail(Long id, Model model) {
        model.addAttribute("payout", payoutService.get(id));
        model.addAttribute("history", payoutService.history(id));
    }
}
