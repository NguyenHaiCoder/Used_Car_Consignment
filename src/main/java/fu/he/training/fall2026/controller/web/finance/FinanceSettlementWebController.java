package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import java.time.LocalDate;
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
 * Vũ Mạnh Cường: Quyết toán và Chi trả chủ xe của Nhân viên tài chính, chạy trên mock {@link FinanceUiData}, chưa ghi DB.
 * Nháp → Chờ duyệt → Đã duyệt → Đã chi. Quản lý từ chối thì quay về Nháp. Chỉ chi khi đã duyệt.
 */
@Controller
@RequestMapping("/finance/settlements")
public class FinanceSettlementWebController {

    private static final String BASE = "/finance/settlements";
    private static final String LIST_VIEW = "finance/settlements/list";
    private static final String DETAIL_VIEW = "finance/settlements/detail";

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", FinanceUiData.SETTLEMENT_STATUSES);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, FinanceUiData.settlements(), "net", "created",
                FinanceUiData.SETTLEMENT_STATUSES, params);
        return LIST_VIEW;
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Map<String, Object> item = find(id);
        model.addAttribute("item", item);
        model.addAttribute("steps", FinanceUiData.settlementSteps(item));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id:\\d+}/submit")
    public String submit(@PathVariable long id, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (FinanceUiData.DRAFT.equals(item.get("status"))) {
            item.put("status", FinanceUiData.PENDING_REVIEW);
            item.put("rejectReason", "");
            flash.addFlashAttribute("flashMessage", "Đã gửi Quản lý duyệt quyết toán " + item.get("code") + ".");
        }
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id:\\d+}/payout")
    public String payout(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String reference,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!FinanceUiData.APPROVED.equals(item.get("status"))) {
            flash.addFlashAttribute("flashError", "Chỉ chi trả khi quyết toán đã được duyệt.");
            return "redirect:" + BASE + "/" + id;
        }
        if (reference.isBlank()) {
            flash.addFlashAttribute("flashError", "Vui lòng nhập mã tham chiếu chi trả.");
            return "redirect:" + BASE + "/" + id;
        }
        item.put("status", FinanceUiData.PAID_OUT);
        item.put("payoutReference", reference.trim());
        item.put("paidOn", LocalDate.now().toString());
        SalesDealUiData.completeConsignment(String.valueOf(item.get("car")));
        flash.addFlashAttribute("flashMessage", "Đã chi trả chủ xe, hồ sơ ký gửi " + item.get("file") + " hoàn thành.");
        return "redirect:" + BASE + "/" + id;
    }

    private static Map<String, Object> find(long id) {
        return FinanceUiData.settlement(id).orElseThrow(() -> new ResourceNotFoundException("Quyết toán", id));
    }
}
