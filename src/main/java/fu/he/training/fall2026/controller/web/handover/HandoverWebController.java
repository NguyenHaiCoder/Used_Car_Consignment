package fu.he.training.fall2026.controller.web.handover;

import fu.he.training.fall2026.controller.web.finance.FinanceUiData;
import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Vũ Mạnh Cường: Bàn giao xe và Đóng sale của Nhân viên bàn giao, chạy trên mock {@link HandoverUiData}, chưa ghi DB.
 * Chờ điều kiện → Sẵn sàng (thu đủ tiền và sang tên xong) → Đã bàn giao → Đã đóng sale.
 * Đóng sale xong thì hợp đồng đóng, hồ sơ ký gửi thành Đã bán và quyết toán tự mở.
 */
@Controller
@RequestMapping("/handover/handovers")
public class HandoverWebController {

    private static final String BASE = "/handover/handovers";
    private static final String LIST_VIEW = "handover/handovers/list";
    private static final String DETAIL_VIEW = "handover/handovers/detail";
    private static final String RECEIPT_VIEW = "handover/handovers/receipt";
    private static final String SIGNATURE_PREFIX = "data:image/png;base64,";

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", HandoverUiData.STATUSES);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, HandoverUiData.rows(), "salePrice", "signedOn", HandoverUiData.STATUSES, params);
        return LIST_VIEW;
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Map<String, Object> contract = findContract(id);
        model.addAttribute("item", find(id));
        model.addAttribute("milestones", SalesDealUiData.saleMilestones(contract));
        model.addAttribute("handoverItems", HandoverUiData.ITEMS);
        return DETAIL_VIEW;
    }

    @PostMapping("/{id:\\d+}/confirm")
    public String confirm(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String odo,
            @RequestParam(defaultValue = "") String keys,
            @RequestParam(required = false) List<String> items,
            @RequestParam(defaultValue = "") String notes,
            @RequestParam(defaultValue = "") String staffSignature,
            @RequestParam(defaultValue = "") String receiverSignature,
            HttpSession session,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!HandoverUiData.READY.equals(item.get("status"))) {
            flash.addFlashAttribute("flashError", "Chỉ bàn giao khi đã thu đủ tiền và sang tên xong.");
            return "redirect:" + BASE + "/" + id;
        }
        Long odometer = SalesDealUiData.parseMoney(odo);
        Long keyCount = SalesDealUiData.parseMoney(keys);
        String error = check(odometer, keyCount, staffSignature, receiverSignature);
        if (error != null) {
            flash.addFlashAttribute("flashError", error);
            return "redirect:" + BASE + "/" + id;
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("staff", SessionAuthUtils.getFullName(session));
        record.put("odo", odometer);
        record.put("keys", keyCount);
        record.put("items", items == null ? List.of() : new ArrayList<>(items));
        record.put("notes", notes.trim());
        record.put("handedOn", LocalDate.now().toString());
        record.put("staffSignature", staffSignature);
        record.put("receiverSignature", receiverSignature);
        HandoverUiData.save(id, record);
        SalesDealUiData.markHandedOver(id);
        flash.addFlashAttribute("flashMessage", "Đã bàn giao xe cho khách.");
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id:\\d+}/close")
    public String close(@PathVariable long id, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!HandoverUiData.COMPLETED.equals(item.get("status"))) {
            flash.addFlashAttribute("flashError", "Chỉ đóng sale khi đã bàn giao xe.");
            return "redirect:" + BASE + "/" + id;
        }
        Map<String, Object> contract = findContract(id);
        SalesDealUiData.closeSale(id);
        FinanceUiData.openSettlement(contract);
        flash.addFlashAttribute("flashMessage", "Đã đóng sale, quyết toán đã mở bên Tài chính.");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id:\\d+}/receipt")
    public String receipt(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!Boolean.TRUE.equals(item.get("handedOver"))) {
            flash.addFlashAttribute("flashError", "Bàn giao xong mới có biên nhận.");
            return "redirect:" + BASE + "/" + id;
        }
        model.addAttribute("item", item);
        return RECEIPT_VIEW;
    }

    private static String check(Long odometer, Long keyCount, String staffSignature, String receiverSignature) {
        if (odometer == null) {
            return "Vui lòng nhập số km lúc bàn giao.";
        }
        if (keyCount == null || keyCount < 1) {
            return "Vui lòng nhập số chìa khóa.";
        }
        return staffSignature.startsWith(SIGNATURE_PREFIX) && receiverSignature.startsWith(SIGNATURE_PREFIX)
                ? null
                : "Cần đủ chữ ký của nhân viên và khách nhận xe.";
    }

    private static Map<String, Object> find(long id) {
        return HandoverUiData.item(id).orElseThrow(() -> new ResourceNotFoundException("Bàn giao", id));
    }

    private static Map<String, Object> findContract(long id) {
        return SalesDealUiData.contract(id).orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }
}
