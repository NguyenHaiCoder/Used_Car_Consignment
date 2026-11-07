package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
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
 * Vũ Mạnh Cường: Giao dịch thanh toán và Hoàn cọc của Nhân viên tài chính, chạy trên mock {@link FinanceUiData}, chưa ghi DB.
 * Sales gửi yêu cầu thu từ hợp đồng đã ký, Finance ghi nhận tay đến khi đủ. Không làm payOS và webhook.
 */
@Controller
@RequestMapping("/finance/payments")
public class FinancePaymentWebController {

    private static final String BASE = "/finance/payments";
    private static final String LIST_VIEW = "finance/payments/list";
    private static final String DETAIL_VIEW = "finance/payments/detail";
    private static final String REFUND_VIEW = "finance/refunds/create";

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", FinanceUiData.PAYMENT_STATUSES);
        model.addAttribute("methods", FinanceUiData.METHODS);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, FinanceUiData.paymentRows(), "amount", "created",
                FinanceUiData.PAYMENT_STATUSES, params);
        return LIST_VIEW;
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Map<String, Object> contract = findContract(id);
        model.addAttribute("item", FinanceUiData.payment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch thanh toán", id)));
        model.addAttribute("receipts", FinanceUiData.receiptsOf(contract));
        model.addAttribute("milestones", SalesDealUiData.saleMilestones(contract));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id:\\d+}/receipts")
    public String addReceipt(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String amount,
            @RequestParam(defaultValue = "") String method,
            @RequestParam(defaultValue = "") String reference,
            HttpSession session,
            RedirectAttributes flash) {
        Map<String, Object> contract = findContract(id);
        Map<String, Object> item = FinanceUiData.payment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch thanh toán", id));
        Long value = SalesDealUiData.parseMoney(amount);
        String error = checkReceipt(value, (Long) item.get("remaining"), method, reference.trim());
        if (error != null) {
            flash.addFlashAttribute("flashError", error);
            return "redirect:" + BASE + "/" + id;
        }
        FinanceUiData.addReceipt(contract, value, method, reference.trim(), SessionAuthUtils.getFullName(session));
        boolean paid = Boolean.TRUE.equals(contract.get("paid"));
        flash.addFlashAttribute("flashMessage", paid
                ? "Đã thu đủ hợp đồng " + contract.get("code") + "."
                : "Đã ghi nhận khoản thu " + SalesDealUiData.money(value) + " ₫.");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/refunds/{id:\\d+}")
    public String refundForm(@PathVariable long id, Model model) {
        model.addAttribute("item", findRefund(id));
        return REFUND_VIEW;
    }

    @PostMapping("/refunds/{id:\\d+}")
    public String refund(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String method,
            @RequestParam(defaultValue = "") String reference,
            @RequestParam(defaultValue = "") String note,
            HttpSession session,
            RedirectAttributes flash) {
        Map<String, Object> item = findRefund(id);
        if (FinanceUiData.REFUNDED.equals(item.get("status"))) {
            flash.addFlashAttribute("flashError", "Phiếu này đã hoàn cọc.");
            return "redirect:" + BASE + "/refunds/" + id;
        }
        String error = checkPaymentInfo(method, reference.trim());
        if (error != null) {
            flash.addFlashAttribute("flashError", error);
            return "redirect:" + BASE + "/refunds/" + id;
        }
        FinanceUiData.addRefund(id, method, reference.trim(), note.trim(), SessionAuthUtils.getFullName(session));
        flash.addFlashAttribute("flashMessage", "Đã ghi nhận hoàn cọc " + item.get("code") + ".");
        return "redirect:" + BASE + "/refunds/" + id;
    }

    private static String checkReceipt(Long amount, long remaining, String method, String reference) {
        if (amount == null || amount <= 0) {
            return "Vui lòng nhập số tiền thu.";
        }
        if (amount > remaining) {
            return "Số tiền thu không được lớn hơn số còn lại.";
        }
        return checkPaymentInfo(method, reference);
    }

    private static String checkPaymentInfo(String method, String reference) {
        if (!FinanceUiData.METHODS.contains(method)) {
            return "Vui lòng chọn phương thức.";
        }
        if (reference.isEmpty()) {
            return "Vui lòng nhập mã tham chiếu.";
        }
        return FinanceUiData.referenceUsed(reference) ? "Mã tham chiếu " + reference + " đã được dùng." : null;
    }

    private static Map<String, Object> findContract(long id) {
        return SalesDealUiData.contract(id).orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }

    private static Map<String, Object> findRefund(long id) {
        return FinanceUiData.refund(id).orElseThrow(() -> new ResourceNotFoundException("Khoản hoàn cọc", id));
    }
}
