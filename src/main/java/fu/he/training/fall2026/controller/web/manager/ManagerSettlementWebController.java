package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.controller.web.finance.FinanceUiData;
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
 * Vũ Mạnh Cường: Quản lý duyệt quyết toán, chạy trên mock {@link FinanceUiData}, chưa ghi DB.
 * Quản lý chỉ duyệt hoặc từ chối, không sửa số tiền. Từ chối phải có lý do và quyết toán quay về Nháp.
 */
@Controller
@RequestMapping("/manager/settlements")
public class ManagerSettlementWebController {

    private static final String BASE = "/manager/settlements";
    private static final String LIST_VIEW = "manager/settlements/list";
    private static final String DETAIL_VIEW = "manager/settlements/detail";

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", FinanceUiData.SETTLEMENT_STATUSES);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, FinanceUiData.settlementsForReview(), "net", "created",
                FinanceUiData.REVIEW_STATUSES, params);
        model.addAttribute("statuses", FinanceUiData.REVIEW_STATUSES);
        return LIST_VIEW;
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("item", find(id));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id:\\d+}/decide")
    public String decide(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String decision,
            @RequestParam(defaultValue = "") String reason,
            HttpSession session,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!FinanceUiData.PENDING_REVIEW.equals(item.get("status"))) {
            flash.addFlashAttribute("flashError", "Quyết toán này đã được xử lý.");
            return "redirect:" + BASE + "/" + id;
        }
        if ("APPROVED".equals(decision)) {
            item.put("status", FinanceUiData.APPROVED);
            item.put("reviewedBy", SessionAuthUtils.getFullName(session));
            flash.addFlashAttribute("flashMessage", "Đã duyệt quyết toán " + item.get("code") + ".");
            return "redirect:" + BASE + "/" + id;
        }
        if (reason.isBlank()) {
            flash.addFlashAttribute("flashError", "Từ chối thì phải nhập lý do.");
            return "redirect:" + BASE + "/" + id;
        }
        item.put("status", FinanceUiData.DRAFT);
        item.put("rejectReason", reason.trim());
        flash.addFlashAttribute("flashMessage", "Đã trả quyết toán " + item.get("code") + " về cho Tài chính.");
        return "redirect:" + BASE;
    }

    private static Map<String, Object> find(long id) {
        return FinanceUiData.settlement(id)
                .filter(s -> !FinanceUiData.DRAFT.equals(s.get("status")))
                .orElseThrow(() -> new ResourceNotFoundException("Quyết toán", id));
    }
}
