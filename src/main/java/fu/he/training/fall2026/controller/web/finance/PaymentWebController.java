package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.dto.finance.PaymentResponse;
import fu.he.training.fall2026.dto.finance.ReceiptRequest;
import fu.he.training.fall2026.dto.finance.RefundRequest;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

/** 05.01 Payment Transaction List / detail and 05.02 Record Receipt / Refund. */
@Controller
@RequestMapping("/finance")
@RequiredArgsConstructor
public class PaymentWebController {

    private static final int PAGE_SIZE = 10;
    private static final String PAYMENTS = "/finance/payments";
    private static final String RECEIPT_VIEW = "finance/receipts/app-create";
    private static final String REFUND_VIEW = "finance/refunds/app-create";

    private final PaymentService paymentService;

    @GetMapping("/payments")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<PaymentResponse> result = paymentService.search(q, status, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        model.addAttribute("counts", paymentService.countByStatus());
        return "finance/payments/app-list";
    }

    @GetMapping("/payments/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("payment", paymentService.get(id));
        model.addAttribute("contract", paymentService.contractOf(id));
        model.addAttribute("checks", paymentService.checks(id));
        model.addAttribute("refunds", paymentService.refunds(id));
        model.addAttribute("history", paymentService.history(id));
        return "finance/payments/app-detail";
    }

    @GetMapping("/receipts/new")
    public String receiptForm(@RequestParam(required = false) Long contractId, Model model) {
        ReceiptRequest form = new ReceiptRequest();
        form.setContractId(contractId);
        form.setPaidAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        model.addAttribute("form", form);
        model.addAttribute("contracts", paymentService.collectableContracts());
        return RECEIPT_VIEW;
    }

    @PostMapping("/payments")
    public String recordReceipt(
            @Valid @ModelAttribute("form") ReceiptRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long id = paymentService.recordReceipt(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã ghi nhận khoản thu, chờ đối soát.");
                return "redirect:" + PAYMENTS + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("contracts", paymentService.collectableContracts());
        return RECEIPT_VIEW;
    }

    @PostMapping("/payments/{id}/reconcile")
    public String reconcile(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã đối soát, khoản thu chuyển PAID.",
                () -> paymentService.reconcile(id, SessionAuthUtils.getUserId(session)));
        return "redirect:" + PAYMENTS + "/" + id;
    }

    @PostMapping("/payments/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã từ chối khoản thu; lịch sử được giữ lại.",
                () -> paymentService.reject(id, reason, SessionAuthUtils.getUserId(session)));
        return "redirect:" + PAYMENTS + "/" + id;
    }

    @GetMapping("/refunds/new")
    public String refundForm(@RequestParam(required = false) Long paymentId, Model model) {
        RefundRequest form = new RefundRequest();
        form.setPaymentId(paymentId);
        model.addAttribute("form", form);
        model.addAttribute("payments", paymentService.refundablePayments());
        return REFUND_VIEW;
    }

    @PostMapping("/refunds")
    public String recordRefund(
            @Valid @ModelAttribute("form") RefundRequest form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long paymentId = paymentService.recordRefund(form, SessionAuthUtils.getUserId(session));
                flash.addFlashAttribute("flashMessage", "Đã ghi nhận yêu cầu hoàn tiền (PENDING).");
                return "redirect:" + PAYMENTS + "/" + paymentId;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("payments", paymentService.refundablePayments());
        return REFUND_VIEW;
    }

    @PostMapping("/refunds/{id}/process")
    public String processRefund(@PathVariable Long id, @RequestParam Long paymentId,
                                @RequestParam(required = false) String evidencePath,
                                HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã hoàn tất hoàn tiền.",
                () -> paymentService.processRefund(id, evidencePath, SessionAuthUtils.getUserId(session)));
        return "redirect:" + PAYMENTS + "/" + paymentId;
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable Long id, @RequestParam Long paymentId,
                               @RequestParam(required = false) String reason,
                               HttpSession session, RedirectAttributes flash) {
        run(flash, "Đã từ chối yêu cầu hoàn tiền.",
                () -> paymentService.rejectRefund(id, reason, SessionAuthUtils.getUserId(session)));
        return "redirect:" + PAYMENTS + "/" + paymentId;
    }

    /** Runs a status action; a broken business rule is shown on the page instead of the error screen. */
    static void run(RedirectAttributes flash, String success, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("flashMessage", success);
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
    }
}
