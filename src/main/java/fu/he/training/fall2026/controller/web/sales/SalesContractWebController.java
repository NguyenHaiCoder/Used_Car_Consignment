package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Đỗ Lan Anh: luồng Hợp đồng bán xe, chạy trên mock trong bộ nhớ ({@link SalesDealUiData}), chưa ghi DB.
 * Chỉ lập được từ phiếu đặt cọc còn hiệu lực (1-1), Sales làm tới bước ký.
 * Từ Thanh toán trở đi là luồng của Vũ Mạnh Cường, nút Thanh toán để sẵn chưa nối.
 */
@Controller
@RequestMapping("/sales/contracts")
public class SalesContractWebController {

    private static final String BASE = "/sales/contracts";
    private static final String LIST_VIEW = "sales/contracts/list";
    private static final String DOC_VIEW = "sales/contracts/create";
    private static final String SIGNATURE_PREFIX = "data:image/png;base64,";

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesDealUiData.CONTRACT_STATUSES);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, SalesDealUiData.contracts(), "salePrice", "created",
                SalesDealUiData.CONTRACT_STATUSES, params);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long reservationId,
            Model model,
            RedirectAttributes flash) {
        if (reservationId == null) {
            model.addAttribute("contract", null);
            model.addAttribute("reservations", SalesDealUiData.activeReservations());
            return DOC_VIEW;
        }
        Map<String, Object> reservation = findReservation(reservationId);
        String redirect = blockedBy(reservation, flash);
        return redirect != null ? redirect : show(SalesDealUiData.draftContract(reservation, now()), null, model);
    }

    @PostMapping
    public String create(
            @RequestParam long reservationId,
            @RequestParam Map<String, String> form,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> reservation = findReservation(reservationId);
        String redirect = blockedBy(reservation, flash);
        if (redirect != null) {
            return redirect;
        }
        Map<String, Object> contract = SalesDealUiData.draftContract(reservation, now());
        String error = apply(contract, form);
        if (error != null) {
            return show(contract, error, model);
        }
        SalesDealUiData.addContract(contract, reservation);
        return finish(contract, flash);
    }

    @GetMapping("/{id}")
    public String open(@PathVariable long id, Model model) {
        return show(find(id), null, model);
    }

    @PostMapping("/{id}")
    public String save(
            @PathVariable long id,
            @RequestParam Map<String, String> form,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> contract = find(id);
        if (!SalesDealUiData.DRAFT.equals(contract.get("status"))) {
            flash.addFlashAttribute("flashError", "Hợp đồng đã ký nên không sửa được.");
            return "redirect:" + BASE + "/" + id;
        }
        Map<String, Object> edited = new LinkedHashMap<>(contract);
        String error = apply(edited, form);
        if (error != null) {
            return show(edited, error, model);
        }
        contract.putAll(edited);
        return finish(contract, flash);
    }

    /** Vũ Mạnh Cường: Sales gửi yêu cầu thu, vì Sales không có quyền mở màn Tài chính. */
    @PostMapping("/{id}/payment-request")
    public String requestPayment(@PathVariable long id, RedirectAttributes flash) {
        Map<String, Object> contract = find(id);
        if (SalesDealUiData.SIGNED.equals(contract.get("status"))) {
            SalesDealUiData.requestPayment(contract);
            flash.addFlashAttribute("flashMessage", "Đã gửi yêu cầu thu tiền sang Tài chính.");
        } else {
            flash.addFlashAttribute("flashError", "Chỉ gửi yêu cầu thu cho hợp đồng đã ký.");
        }
        return "redirect:" + BASE + "/" + id;
    }

    private static String finish(Map<String, Object> contract, RedirectAttributes flash) {
        flash.addFlashAttribute("flashMessage", SalesDealUiData.SIGNED.equals(contract.get("status"))
                ? "Đã ký hợp đồng " + contract.get("code") + "."
                : "Đã lưu nháp hợp đồng " + contract.get("code") + ".");
        return "redirect:" + BASE + "/" + contract.get("id");
    }

    /** Ghi nội dung form vào hợp đồng; bấm Ký hợp đồng thì kiểm tra thêm thông tin người mua và chữ ký hai bên. */
    private static String apply(Map<String, Object> contract, Map<String, String> form) {
        for (String key : List.of("buyerIdNumber", "buyerAddress", "paymentTerms", "handoverTerms", "terminationTerms")) {
            contract.put(key, form.getOrDefault(key, "").trim());
        }
        Long salePrice = SalesDealUiData.parseMoney(form.get("salePrice"));
        long deposit = (Long) contract.get("deposit");
        if (salePrice != null) {
            contract.put("salePrice", salePrice);
            contract.put("remaining", salePrice - deposit);
        }
        if (salePrice == null || salePrice <= deposit) {
            return "Giá bán phải lớn hơn tiền cọc đã đặt.";
        }
        if (!"sign".equals(form.get("intent"))) {
            return null;
        }
        if (!form.getOrDefault("buyerIdNumber", "").trim().matches("\\d{12}")) {
            return "Số CCCD của người mua phải đủ 12 số.";
        }
        if (form.getOrDefault("buyerAddress", "").isBlank()) {
            return "Vui lòng nhập địa chỉ người mua.";
        }
        String seller = form.getOrDefault("sellerSignature", "");
        String buyer = form.getOrDefault("buyerSignature", "");
        if (!seller.startsWith(SIGNATURE_PREFIX) || !buyer.startsWith(SIGNATURE_PREFIX)) {
            return "Hợp đồng cần đủ chữ ký của hai bên.";
        }
        contract.put("sellerSignature", seller);
        contract.put("buyerSignature", buyer);
        contract.put("status", SalesDealUiData.SIGNED);
        contract.put("signedOn", LocalDate.now().toString());
        return null;
    }

    private static String show(Map<String, Object> contract, String error, Model model) {
        model.addAttribute("contract", contract);
        model.addAttribute("formError", error);
        model.addAttribute("editable", SalesDealUiData.DRAFT.equals(contract.get("status")));
        model.addAttribute("isSigned", !String.valueOf(contract.get("signedOn")).isEmpty());
        model.addAttribute("steps", steps(contract));
        return DOC_VIEW;
    }

    /** Ba mốc cuối do luồng khác cập nhật, đủ cả ba thì mới đóng sale. */
    private static List<Map<String, String>> steps(Map<String, Object> contract) {
        boolean signed = !String.valueOf(contract.get("signedOn")).isEmpty();
        List<Map<String, String>> steps = new ArrayList<>();
        steps.add(step("Tư vấn", String.valueOf(contract.get("inquiryCode")), "done"));
        steps.add(step("Đặt cọc giữ xe", String.valueOf(contract.get("reservationCode")), "done"));
        steps.add(step("Ký hợp đồng", signed ? "Đã ký hai bên" : "Chờ ký", signed ? "done" : "current"));
        steps.add(milestone("Thanh toán", (Boolean) contract.get("paid")));
        steps.add(milestone("Sang tên", (Boolean) contract.get("transferred")));
        steps.add(milestone("Bàn giao xe", (Boolean) contract.get("handedOver")));
        return steps;
    }

    private static Map<String, String> milestone(String label, boolean done) {
        return step(label, done ? "Đã hoàn tất" : "Chưa thực hiện", done ? "done" : "");
    }

    private static Map<String, String> step(String label, String note, String state) {
        return Map.of("label", label, "note", note, "state", state);
    }

    /** Phiếu đã có hợp đồng thì mở lại hợp đồng đó, phiếu đã kết thúc thì quay về phiếu. Hợp lệ thì trả về null. */
    private static String blockedBy(Map<String, Object> reservation, RedirectAttributes flash) {
        var existing = SalesDealUiData.contractOf((Long) reservation.get("id"));
        if (existing.isPresent()) {
            flash.addFlashAttribute("flashError", "Phiếu đặt cọc này đã có hợp đồng, mình mở lại để cập nhật tiếp.");
            return "redirect:" + BASE + "/" + existing.get().get("id");
        }
        if (!SalesDealUiData.ACTIVE.equals(reservation.get("status"))) {
            flash.addFlashAttribute("flashError", "Chỉ lập được hợp đồng từ phiếu đặt cọc còn hiệu lực.");
            return "redirect:/sales/reservations/" + reservation.get("id");
        }
        return null;
    }

    private static String now() {
        return LocalDateTime.now().withSecond(0).withNano(0).toString();
    }

    private static Map<String, Object> find(long id) {
        return SalesDealUiData.contract(id).orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán xe", id));
    }

    private static Map<String, Object> findReservation(long id) {
        return SalesDealUiData.reservation(id).orElseThrow(() -> new ResourceNotFoundException("Phiếu đặt cọc", id));
    }
}
