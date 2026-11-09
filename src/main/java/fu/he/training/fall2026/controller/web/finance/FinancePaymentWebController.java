package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.finance.DealFilterSupport;
import fu.he.training.fall2026.dto.finance.PaymentDealRow;
import fu.he.training.fall2026.dto.finance.PaymentFilter;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.dto.finance.RefundView;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
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
 * Vũ Mạnh Cường: Giao dịch thanh toán và Hoàn cọc — Controller → PaymentService → DB.
 */
@Controller
@RequestMapping("/finance/payments")
@RequiredArgsConstructor
public class FinancePaymentWebController {

    private static final String BASE = "/finance/payments";
    private static final String LIST_VIEW = "finance/payments/list";
    private static final String DETAIL_VIEW = "finance/payments/detail";
    private static final String REFUND_VIEW = "finance/refunds/create";

    private final PaymentService paymentService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", paymentService.paymentStatusMeta());
        model.addAttribute("methods", PaymentService.METHODS);
    }

    @RequirePermission(Perm.PAYMENT_VIEW)
    @GetMapping
    public String list(
            @ModelAttribute("filter") PaymentFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<PaymentDealRow> result = CatalogPageSupport.lastPageIfBeyond(
                paymentService.searchDeals(filter, page, pageSize),
                last -> paymentService.searchDeals(filter, last, pageSize));
        DealFilterSupport.bindListModel(model, BASE, filter, pageSize);
        model.addAttribute("page", result);
        model.addAttribute("counts", paymentService.countDealStatuses());
        model.addAttribute("staffs", paymentService.listStaffNames());
        return LIST_VIEW;
    }

    @RequirePermission(Perm.PAYMENT_VIEW)
    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        PaymentDealRow item = paymentService.getPaymentDeal(id);
        model.addAttribute("item", item);
        model.addAttribute("receipts", paymentService.receiptsOf(id));
        model.addAttribute("milestones", paymentService.saleMilestones(id));
        model.addAttribute("form", new ReceiptRequest());
        return DETAIL_VIEW;
    }

    @RequirePermission(Perm.PAYMENT_RECORD)
    @PostMapping("/{id}/receipts")
    public String addReceipt(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ReceiptRequest form,
            BindingResult errors,
            HttpSession session,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("flashError", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:" + BASE + "/" + id;
        }
        try {
            paymentService.recordReceipt(id, form, SessionAuthUtils.getFullName(session));
            boolean paid = paymentService.isPaid(id);
            flash.addFlashAttribute("flashMessage", paid
                    ? "Đã thu đủ hợp đồng."
                    : "Đã ghi nhận khoản thu.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @RequirePermission(Perm.REFUND_PROCESS)
    @GetMapping("/refunds/{id}")
    public String refundForm(@PathVariable long id, Model model) {
        model.addAttribute("item", paymentService.getRefund(id));
        model.addAttribute("form", new RefundRequest());
        return REFUND_VIEW;
    }

    @RequirePermission(Perm.REFUND_PROCESS)
    @PostMapping("/refunds/{id}")
    public String refund(
            @PathVariable long id,
            @Valid @ModelAttribute("form") RefundRequest form,
            BindingResult errors,
            HttpSession session,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("flashError", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:" + BASE + "/refunds/" + id;
        }
        try {
            paymentService.processRefund(id, form, SessionAuthUtils.getFullName(session));
            RefundView item = paymentService.getRefund(id);
            flash.addFlashAttribute("flashMessage", "Đã ghi nhận hoàn cọc " + item.getCode() + ".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/refunds/" + id;
    }
}
