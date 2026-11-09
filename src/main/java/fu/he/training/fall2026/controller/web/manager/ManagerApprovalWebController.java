package fu.he.training.fall2026.controller.web.manager;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.service.ManagerApprovalService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.PermissionGuard;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
 * Nguyễn Việt Hải: Hàng đợi phê duyệt của Quản lý, dữ liệu thật từ {@link ManagerApprovalService}.
 * Còn điều kiện chưa đạt thì không duyệt được. Từ chối và yêu cầu bổ sung phải có lý do.
 */
@Controller
@RequestMapping("/manager/approvals")
@RequiredArgsConstructor
public class ManagerApprovalWebController {

    private static final String BASE = "/manager/approvals";
    private static final String LIST_VIEW = "manager/approvals/list";
    private static final String DETAIL_VIEW = "manager/approvals/detail";

    private final ManagerApprovalService approvalService;
    private final PermissionGuard permissionGuard;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", approvalService.statusMeta());
        model.addAttribute("types", approvalService.typeMeta());
    }

    @RequirePermission(Perm.MANAGER_APPROVAL_VIEW)
    @GetMapping
    public String list(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        return renderList(null, BASE, params, session, model);
    }

    @RequirePermission(Perm.MANAGER_APPROVAL_VIEW)
    @GetMapping("/type/{type:[A-Z_]+}")
    public String listByType(
            @PathVariable String type,
            @RequestParam Map<String, String> params,
            HttpSession session,
            Model model) {
        if (!approvalService.typeMeta().containsKey(type)) {
            throw new ResourceNotFoundException("Loại phê duyệt", type);
        }
        return renderList(type, BASE + "/type/" + type, params, session, model);
    }

    @RequirePermission(Perm.MANAGER_APPROVAL_VIEW)
    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Map<String, Object> item = approvalService.detail(id);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> checks = (List<Map<String, String>>) item.get("checks");
        model.addAttribute("item", item);
        model.addAttribute("open", ManagerApprovalService.PENDING.equals(item.get("status"))
                && !"SETTLEMENT".equals(item.get("type")));
        model.addAttribute("blockers", checks.stream().filter(c -> !"true".equals(c.get("passed"))).count());
        return DETAIL_VIEW;
    }

    @PostMapping("/{id:\\d+}/decide")
    public String decide(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String decision,
            @RequestParam(defaultValue = "") String note,
            HttpSession session,
            RedirectAttributes flash) {
        // Nguyễn Việt Hải: mỗi loại yêu cầu một quyền duyệt riêng, bị tắt thì về lại trang kèm toast.
        permissionGuard.require(Perm.forApproval(approvalService.typeOf(id)));
        try {
            String code = approvalService.decide(id, decision, note, SessionAuthUtils.getUserId(session));
            String message = switch (decision) {
                case "APPROVED" -> "Đã duyệt yêu cầu ";
                case "REJECTED" -> "Đã từ chối yêu cầu ";
                default -> "Đã yêu cầu bổ sung cho ";
            };
            flash.addFlashAttribute("flashMessage", message + code + ".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    private String renderList(
            String type, String base, Map<String, String> params, HttpSession session, Model model) {
        List<Map<String, Object>> all = approvalService.queue(SessionAuthUtils.getUserId(session));
        List<Map<String, Object>> rows = all.stream()
                .filter(r -> type == null || type.equals(r.get("type")))
                .toList();
        bindList(model, base, rows, approvalService.statusMeta(), params);
        model.addAttribute("type", type);
        model.addAttribute("pending", pendingByType(all));
        return LIST_VIEW;
    }

    /** Số yêu cầu đang chờ của từng loại, khóa rỗng là tổng. Loại nào cũng có khóa để template so sánh không lỗi. */
    private Map<String, Long> pendingByType(List<Map<String, Object>> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("", rows.stream().filter(r -> ManagerApprovalService.PENDING.equals(r.get("status"))).count());
        approvalService.typeMeta().keySet().forEach(key -> counts.put(key, rows.stream()
                .filter(r -> key.equals(r.get("type")) && ManagerApprovalService.PENDING.equals(r.get("status")))
                .count()));
        return counts;
    }

    /** Lọc, phân trang và đưa vào Model các biến mà sales/deal-parts đang đọc. */
    private static void bindList(
            Model model,
            String base,
            List<Map<String, Object>> rows,
            Map<String, Map<String, String>> statuses,
            Map<String, String> params) {
        String keyword = params.getOrDefault("q", "").trim();
        String lower = keyword.toLowerCase(Locale.ROOT);
        String status = params.getOrDefault("status", "").trim();
        String staff = params.getOrDefault("staff", "").trim();
        String dateFrom = isoDate(params.get("from"));
        String dateTo = isoDate(params.get("to"));
        Long amountMin = million(params.get("amountMin"));
        Long amountMax = million(params.get("amountMax"));

        List<Map<String, Object>> shown = rows.stream()
                .filter(r -> lower.isEmpty() || haystack(r).contains(lower))
                .filter(r -> status.isEmpty() || status.equals(r.get("status")))
                .filter(r -> staff.isEmpty() || staff.equals(r.get("staff")))
                .filter(r -> dateFrom.isEmpty() || day(r).compareTo(dateFrom) >= 0)
                .filter(r -> dateTo.isEmpty() || day(r).compareTo(dateTo) <= 0)
                .filter(r -> amountMin == null || amount(r) >= amountMin)
                .filter(r -> amountMax == null || amount(r) <= amountMax)
                .toList();

        Integer size = number(params.get("size"));
        Integer wanted = number(params.get("page"));
        Page<Map<String, Object>> page = CatalogPageSupport.pageOf(shown, wanted == null ? 0 : wanted, size);

        Map<String, Long> counts = new LinkedHashMap<>();
        statuses.keySet().forEach(key -> counts.put(key, rows.stream().filter(r -> key.equals(r.get("status"))).count()));

        model.addAttribute("base", base);
        model.addAttribute("page", page);
        model.addAttribute("counts", counts);
        model.addAttribute("staffs", rows.stream().map(r -> String.valueOf(r.get("staff")))
                .filter(s -> !s.isBlank()).distinct().sorted().toList());
        model.addAttribute("q", keyword);
        model.addAttribute("status", status);
        model.addAttribute("staff", staff);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("amountMin", amountMin == null ? "" : params.get("amountMin"));
        model.addAttribute("amountMax", amountMax == null ? "" : params.get("amountMax"));
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
    }

    private static String haystack(Map<String, Object> row) {
        return String.join(" ", List.of("code", "target", "car", "plate", "staff").stream()
                .map(row::get).filter(Objects::nonNull).map(String::valueOf).toList()).toLowerCase(Locale.ROOT);
    }

    private static String day(Map<String, Object> row) {
        String created = String.valueOf(row.get("created"));
        return created.length() >= 10 ? created.substring(0, 10) : created;
    }

    private static long amount(Map<String, Object> row) {
        return row.get("amount") instanceof Number number ? number.longValue() : 0L;
    }

    private static String isoDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "";
        }
        try {
            LocalDate.parse(text);
            return text;
        } catch (java.time.format.DateTimeParseException ex) {
            return "";
        }
    }

    private static Long million(String text) {
        Integer value = number(text);
        return value == null ? null : value * 1_000_000L;
    }

    private static Integer number(String text) {
        return text == null || !text.matches("\\d{1,5}") ? null : Integer.valueOf(text);
    }
}
