package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
 * Đỗ Lan Anh: luồng Đặt cọc giữ xe, chạy trên mock trong bộ nhớ ({@link SalesDealUiData}), chưa ghi DB.
 * Chọn xe từ hồ sơ ký gửi rồi chọn khách đang tư vấn xe đó. Thu tiền cọc thực tế thuộc luồng Thanh toán.
 */
@Controller
@RequestMapping("/sales/reservations")
public class SalesReservationWebController {

    private static final String BASE = "/sales/reservations";
    private static final String LIST_VIEW = "sales/reservations/list";
    private static final String FORM_VIEW = "sales/reservations/form";
    private static final String DETAIL_VIEW = "sales/reservations/detail";
    private static final int DEFAULT_HOLD_DAYS = 7;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesDealUiData.HOLD_STATUSES);
        model.addAttribute("decisions", SalesDealUiData.DEPOSIT_DECISIONS);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, SalesDealUiData.reservations(), "deposit", "holdUntil",
                SalesDealUiData.HOLD_STATUSES, params);
        model.addAttribute("contractIds", contractIds());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(@RequestParam Map<String, String> params, Model model, RedirectAttributes flash) {
        Map<String, String> form = new HashMap<>(params);
        if (params.containsKey("inquiryId")) {
            Map<String, Object> inquiry = findInquiry(params.get("inquiryId"));
            String error = inquiry == null ? "Không tìm thấy tư vấn." : holdError(inquiry);
            if (error != null) {
                flash.addFlashAttribute("flashError", error);
                return inquiry == null ? "redirect:" + BASE : "redirect:/sales/inquiries/" + inquiry.get("id");
            }
            form.put("car", String.valueOf(inquiry.get("car")));
        }
        form.putIfAbsent("holdUntil", LocalDate.now().plusDays(DEFAULT_HOLD_DAYS).toString());
        form.putIfAbsent("terms", SalesDealUiData.HOLD_TERMS);
        return showCreate(form, null, model);
    }

    @PostMapping
    public String create(@RequestParam Map<String, String> form, Model model, RedirectAttributes flash) {
        Map<String, Object> car = SalesDealUiData.availableCars().get(form.getOrDefault("car", ""));
        if (car == null) {
            form.remove("car");
            return showCreate(form, "Xe này đã được giữ hoặc đã bán, vui lòng chọn xe khác.", model);
        }
        Map<String, Object> inquiry = findInquiry(form.get("inquiryId"));
        if (inquiry == null || !form.get("car").equals(inquiry.get("car")) || !SalesDealUiData.canHold(inquiry)) {
            return showCreate(form, "Vui lòng chọn khách hàng đang được tư vấn xe này.", model);
        }
        long price = ((Number) car.get("price")).longValue();
        String error = checkDeposit(form, price);
        if (error == null && !form.containsKey("consent")) {
            error = "Khách cần đồng ý lưu thông tin và điều khoản giữ xe.";
        }
        if (error != null) {
            return showCreate(form, error, model);
        }

        long id = (Long) inquiry.get("id");
        Map<String, Object> saved = SalesDealUiData.addReservation(inquiry,
                SalesDealUiData.parseMoney(form.get("deposit")), form.get("holdUntil"),
                form.getOrDefault("terms", "").trim(), LocalDateTime.now().withSecond(0).withNano(0).toString());
        SalesInquiryUiData.viewingsOf(id).stream()
                .filter(SalesInquiryUiData::isActive)
                .forEach(v -> v.put("status", SalesInquiryUiData.CANCELLED));
        inquiry.put("status", SalesInquiryUiData.CONVERTED);
        flash.addFlashAttribute("flashMessage", "Đã tạo phiếu đặt cọc " + saved.get("code") + ", có thể lập hợp đồng bán xe.");
        return "redirect:" + BASE + "/" + saved.get("id");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, @RequestParam(required = false) String cancel, Model model) {
        Map<String, Object> item = find(id);
        boolean canDecide = canDecide(item);
        model.addAttribute("item", item);
        model.addAttribute("contract", SalesDealUiData.contractOf(id).orElse(null));
        model.addAttribute("canDecide", canDecide);
        model.addAttribute("openDialog", cancel != null && canDecide);
        return DETAIL_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!SalesDealUiData.ACTIVE.equals(item.get("status"))) {
            return ended(id, flash);
        }
        Map<String, String> form = new HashMap<>();
        form.put("deposit", SalesDealUiData.money((Long) item.get("deposit")));
        form.put("holdUntil", String.valueOf(item.get("holdUntil")));
        form.put("terms", String.valueOf(item.get("terms")));
        return showEdit(item, form, null, model);
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable long id,
            @RequestParam Map<String, String> form,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!SalesDealUiData.ACTIVE.equals(item.get("status"))) {
            return ended(id, flash);
        }
        String error = checkDeposit(form, ((Number) item.get("price")).longValue());
        if (error != null) {
            return showEdit(item, form, error, model);
        }
        item.put("deposit", SalesDealUiData.parseMoney(form.get("deposit")));
        item.put("holdUntil", form.get("holdUntil"));
        item.put("terms", form.getOrDefault("terms", "").trim());
        flash.addFlashAttribute("flashMessage", "Đã cập nhật phiếu đặt cọc " + item.get("code") + ".");
        return "redirect:" + BASE + "/" + id;
    }

    /** Phiếu đang giữ thì hủy; phiếu hết hạn chỉ ghi nhận hoàn hay giữ cọc. Finance sẽ tạo Refund từ quyết định này. */
    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(defaultValue = "") String decision,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (!canDecide(item)) {
            return ended(id, flash);
        }
        if (reason.isBlank() || !SalesDealUiData.DEPOSIT_DECISIONS.containsKey(decision)) {
            flash.addFlashAttribute("flashError", "Vui lòng nhập lý do và chọn hoàn hay giữ cọc.");
            return "redirect:" + BASE + "/" + id;
        }
        item.put("cancelReason", reason.trim());
        item.put("depositDecision", decision);
        if (SalesDealUiData.ACTIVE.equals(item.get("status"))) {
            long inquiryId = (Long) item.get("inquiryId");
            item.put("status", SalesDealUiData.CANCELLED);
            item.put("cancelledAt", LocalDateTime.now().withSecond(0).withNano(0).toString());
            SalesInquiryUiData.inquiry(inquiryId).ifPresent(
                    i -> i.put("status", SalesInquiryUiData.statusWithoutActiveStep(inquiryId)));
            flash.addFlashAttribute("flashMessage", "Đã hủy phiếu đặt cọc " + item.get("code") + ", xe được mở bán lại.");
        } else {
            flash.addFlashAttribute("flashMessage", "Đã ghi nhận cách xử lý tiền cọc của phiếu " + item.get("code") + ".");
        }
        return "redirect:" + BASE + "/" + id;
    }

    private String showCreate(Map<String, String> form, String error, Model model) {
        String title = form.getOrDefault("car", "");
        Map<String, Map<String, Object>> cars = SalesDealUiData.availableCars();
        Map<String, Object> picked = cars.get(title);
        if (picked != null) {
            long price = ((Number) picked.get("price")).longValue();
            form.putIfAbsent("deposit", SalesDealUiData.money(price / 10 / 1_000_000 * 1_000_000));
        }
        model.addAttribute("cars", cars);
        model.addAttribute("picked", picked);
        model.addAttribute("carTitle", picked == null ? "" : title);
        model.addAttribute("buyers", picked == null ? List.of() : SalesDealUiData.inquiriesFor(title));
        return showForm(null, form, error, model);
    }

    private String showEdit(Map<String, Object> item, Map<String, String> form, String error, Model model) {
        model.addAttribute("picked", item);
        model.addAttribute("carTitle", item.get("car"));
        return showForm(item, form, error, model);
    }

    private String showForm(Map<String, Object> item, Map<String, String> form, String error, Model model) {
        model.addAttribute("item", item);
        model.addAttribute("form", form);
        model.addAttribute("formError", error);
        model.addAttribute("today", LocalDate.now().toString());
        return FORM_VIEW;
    }

    private static Map<String, Object> findInquiry(String id) {
        Long inquiryId = SalesDealUiData.parseId(id);
        return inquiryId == null ? null : SalesInquiryUiData.inquiry(inquiryId).orElse(null);
    }

    private static String holdError(Map<String, Object> inquiry) {
        if (SalesInquiryUiData.NEW.equals(inquiry.get("status"))) {
            return "Cần nhận tư vấn trước khi đặt cọc.";
        }
        if (!SalesDealUiData.canHold(inquiry)) {
            return "Tư vấn này đã kết thúc nên không đặt cọc được.";
        }
        return SalesDealUiData.carHeld(String.valueOf(inquiry.get("car")))
                ? "Xe " + inquiry.get("car") + " đã được giữ hoặc đã bán."
                : null;
    }

    private static String checkDeposit(Map<String, String> form, long price) {
        Long deposit = SalesDealUiData.parseMoney(form.get("deposit"));
        if (deposit == null || deposit <= 0) {
            return "Vui lòng nhập tiền cọc.";
        }
        if (deposit >= price) {
            return "Tiền cọc phải nhỏ hơn giá niêm yết của xe.";
        }
        LocalDate holdUntil = SalesDealUiData.parseDate(form.get("holdUntil"));
        if (holdUntil == null) {
            return "Vui lòng chọn hạn giữ xe.";
        }
        return holdUntil.isAfter(LocalDate.now()) ? null : "Hạn giữ xe phải sau hôm nay.";
    }

    private static boolean canDecide(Map<String, Object> item) {
        return SalesDealUiData.ACTIVE.equals(item.get("status"))
                || (SalesDealUiData.EXPIRED.equals(item.get("status")) && "".equals(item.get("depositDecision")));
    }

    private static Map<Long, Long> contractIds() {
        Map<Long, Long> ids = new HashMap<>();
        SalesDealUiData.contracts().forEach(c -> ids.put((Long) c.get("reservationId"), (Long) c.get("id")));
        return ids;
    }

    private static String ended(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Phiếu đặt cọc này đã kết thúc nên không thay đổi được.");
        return "redirect:" + BASE + "/" + id;
    }

    private static Map<String, Object> find(long id) {
        return SalesDealUiData.reservation(id).orElseThrow(() -> new ResourceNotFoundException("Phiếu đặt cọc", id));
    }
}
