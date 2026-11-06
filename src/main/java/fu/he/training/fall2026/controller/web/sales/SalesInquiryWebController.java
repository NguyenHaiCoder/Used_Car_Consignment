package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * Đỗ Lan Anh: luồng Tư vấn &amp; lịch xem xe của Nhân viên kinh doanh, chạy trên mock trong bộ nhớ ({@link SalesInquiryUiData}).
 * Chưa ghi DB. Trạng thái đi theo: Yêu cầu mới → Đang tư vấn → Đã đặt lịch → Đã xem xe, có thể đóng ở mọi bước.
 */
@Controller
@RequestMapping("/sales/inquiries")
public class SalesInquiryWebController {

    private static final String BASE = "/sales/inquiries";
    private static final String LIST_VIEW = "sales/inquiries/list";
    private static final String CREATE_VIEW = "sales/inquiries/create";
    private static final String DETAIL_VIEW = "sales/inquiries/detail";
    private static final String EDIT_VIEW = "sales/inquiries/edit";
    private static final String VIEWING_VIEW = "sales/inquiries/viewing";
    private static final String PHONE_PATTERN = "(0|\\+?84)\\d{9}";
    private static final List<Integer> DURATIONS = List.of(30, 45, 60, 90);

    /** Danh mục hiển thị dùng chung cho mọi màn của luồng. */
    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesInquiryUiData.STATUSES);
        model.addAttribute("viewingNames", SalesInquiryUiData.VIEWING_NAMES);
        model.addAttribute("viewingPills", SalesInquiryUiData.VIEWING_PILLS);
        model.addAttribute("channels", SalesInquiryUiData.CHANNELS);
        model.addAttribute("needs", SalesInquiryUiData.NEEDS);
        model.addAttribute("dayNames", List.of("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"));
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String viewing,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String need,
            @RequestParam(required = false) Long selected,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim();
        String lower = keyword.toLowerCase(Locale.ROOT);
        String statusFilter = blank(status);
        String viewingFilter = blank(viewing);
        String channelFilter = blank(channel);
        String needFilter = blank(need);
        String dateFrom = isoDate(from);
        String dateTo = isoDate(to);
        Map<Long, Map<String, Object>> latest = SalesInquiryUiData.latestViewings();

        List<Map<String, Object>> filtered = SalesInquiryUiData.inquiries().stream()
                .filter(i -> lower.isEmpty() || haystack(i).contains(lower))
                .filter(i -> statusFilter.isEmpty() || statusFilter.equals(i.get("status")))
                .filter(i -> channelFilter.isEmpty() || channelFilter.equals(i.get("channel")))
                .filter(i -> needFilter.isEmpty() || needFilter.equals(i.get("need")))
                .filter(i -> viewingFilter.isEmpty() || (latest.containsKey(i.get("id"))
                        && viewingFilter.equals(latest.get(i.get("id")).get("status"))))
                .filter(i -> dateFrom.isEmpty() || createdDay(i).compareTo(dateFrom) >= 0)
                .filter(i -> dateTo.isEmpty() || createdDay(i).compareTo(dateTo) <= 0)
                .toList();

        Page<Map<String, Object>> result = CatalogPageSupport.pageOf(filtered, page, size);
        Map<String, Object> current = result.getContent().stream()
                .filter(i -> i.get("id").equals(selected))
                .findFirst()
                .orElse(result.hasContent() ? result.getContent().get(0) : null);

        Map<String, Long> counts = new LinkedHashMap<>();
        SalesInquiryUiData.STATUSES.keySet().forEach(key -> counts.put(key, SalesInquiryUiData.inquiries().stream()
                .filter(i -> key.equals(i.get("status"))).count()));

        model.addAttribute("page", result);
        model.addAttribute("counts", counts);
        model.addAttribute("q", keyword);
        model.addAttribute("status", statusFilter);
        model.addAttribute("viewing", viewingFilter);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("channel", channelFilter);
        model.addAttribute("need", needFilter);
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
        model.addAttribute("viewings", latest);
        model.addAttribute("cur", current);
        model.addAttribute("curView", current == null ? null : latest.get(current.get("id")));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(@RequestParam Map<String, String> params, Model model) {
        Map<String, String> form = new HashMap<>(params);
        form.putIfAbsent("channel", "PHONE");
        form.putIfAbsent("need", "PRICE");
        return showCreate(form, null, model);
    }

    @PostMapping
    public String create(
            @RequestParam Map<String, String> form,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        String phone = form.getOrDefault("phone", "").trim();
        String car = form.getOrDefault("car", "");
        boolean knownBuyer = SalesInquiryUiData.buyerByPhone(phone).isPresent();
        if (!SalesInquiryUiData.digits(phone).matches(PHONE_PATTERN.replace("\\+?", ""))) {
            return showCreate(form, "Số điện thoại chưa đúng. Nhập 10 số hoặc bắt đầu bằng +84.", model);
        }
        if (form.getOrDefault("fullName", "").trim().length() < 2) {
            return showCreate(form, "Vui lòng nhập họ và tên khách hàng.", model);
        }
        if (!SalesInquiryUiData.CARS.containsKey(car)) {
            return showCreate(form, "Vui lòng chọn xe khách quan tâm.", model);
        }
        if (!knownBuyer && !form.containsKey("consent")) {
            return showCreate(form, "Khách mới cần đồng ý cho lưu thông tin.", model);
        }
        var existing = SalesInquiryUiData.openInquiryOf(phone, car);
        if (existing.isPresent()) {
            flash.addFlashAttribute("flashError",
                    "Khách này đã có tư vấn cho xe " + car + ", mình mở lại để cập nhật tiếp.");
            return "redirect:" + BASE + "/" + existing.get().get("id");
        }

        String staff = SessionAuthUtils.getFullName(session);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("buyer", form.get("fullName").trim());
        row.put("phone", phone);
        row.put("car", car);
        row.put("status", SalesInquiryUiData.OPEN);
        row.put("channel", form.getOrDefault("channel", "PHONE"));
        row.put("need", form.getOrDefault("need", "PRICE"));
        row.put("staff", staff == null ? "" : staff);
        row.put("created", LocalDateTime.now().withSecond(0).withNano(0).toString());
        applyDetails(row, form);
        Map<String, Object> saved = SalesInquiryUiData.addInquiry(row);
        flash.addFlashAttribute("flashMessage", "Đã tạo tư vấn " + saved.get("code") + ".");
        return "redirect:" + BASE + "/" + saved.get("id");
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) String close,
            Model model) {
        Map<String, Object> item = find(id);
        model.addAttribute("item", item);
        model.addAttribute("viewingList", SalesInquiryUiData.viewingsOf(id));
        model.addAttribute("closeReasons", SalesInquiryUiData.CLOSE_REASONS);
        model.addAttribute("locked", isLocked(item));
        model.addAttribute("openClose", close != null && !isLocked(item));
        model.addAttribute("reservation", SalesDealUiData.latestReservationOf(id).orElse(null));
        model.addAttribute("canHold", SalesDealUiData.canHold(item)
                && !SalesDealUiData.carHeld(String.valueOf(item.get("car"))));
        return DETAIL_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        model.addAttribute("item", item);
        return EDIT_VIEW;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable long id,
            @RequestParam Map<String, String> form,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        if (form.getOrDefault("fullName", "").trim().length() < 2) {
            flash.addFlashAttribute("flashError", "Vui lòng nhập họ và tên khách hàng.");
            return "redirect:" + BASE + "/" + id + "/edit";
        }
        item.put("buyer", form.get("fullName").trim());
        item.put("ini", SalesInquiryUiData.initials(form.get("fullName")));
        item.put("need", form.getOrDefault("need", String.valueOf(item.get("need"))));
        applyDetails(item, form);
        flash.addFlashAttribute("flashMessage", "Đã cập nhật tư vấn " + item.get("code") + ".");
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/claim")
    public String claim(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (SalesInquiryUiData.NEW.equals(item.get("status"))) {
            String staff = SessionAuthUtils.getFullName(session);
            item.put("staff", staff);
            item.put("staffIni", SalesInquiryUiData.initials(staff));
            item.put("status", SalesInquiryUiData.OPEN);
            flash.addFlashAttribute("flashMessage", "Đã nhận tư vấn " + item.get("code") + ".");
        }
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/notes")
    public String saveNote(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String note,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        item.put("note", note.trim());
        flash.addFlashAttribute("flashMessage", "Đã lưu ghi chú tư vấn.");
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(
            @PathVariable long id,
            @RequestParam String closeReason,
            @RequestParam(defaultValue = "") String note,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        SalesInquiryUiData.viewingsOf(id).stream()
                .filter(SalesInquiryUiData::isActive)
                .forEach(v -> v.put("status", SalesInquiryUiData.CANCELLED));
        item.put("status", SalesInquiryUiData.CLOSED);
        item.put("closeReason", closeReason);
        if (!note.isBlank()) {
            item.put("note", note.trim());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id}/viewings/new")
    public String viewingForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        if (SalesInquiryUiData.NEW.equals(item.get("status"))) {
            return notClaimed(id, flash);
        }
        Map<String, String> form = new HashMap<>();
        form.put("date", LocalDate.now().plusDays(1).toString());
        form.put("time", "09:00");
        form.put("minutes", "45");
        form.put("type", "TEST_DRIVE".equals(item.get("need")) ? "TEST_DRIVE" : "VIEWING");
        form.put("place", SalesInquiryUiData.PLACES.get(0));
        form.put("note", "");
        return showViewing(item, null, form, null, model);
    }

    @PostMapping("/{id}/viewings")
    public String addViewing(
            @PathVariable long id,
            @RequestParam Map<String, String> form,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        if (SalesInquiryUiData.NEW.equals(item.get("status"))) {
            return notClaimed(id, flash);
        }
        String error = checkSchedule(item, form, null);
        if (error != null) {
            return showViewing(item, null, form, error, model);
        }
        Map<String, Object> viewing = SalesInquiryUiData.addViewing(id);
        applySchedule(viewing, form);
        item.put("status", SalesInquiryUiData.SCHEDULED);
        flash.addFlashAttribute("flashMessage", "Đã đặt lịch xem xe cho " + item.get("buyer") + ".");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id}/viewings/{viewingId}/edit")
    public String rescheduleForm(
            @PathVariable long id,
            @PathVariable long viewingId,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        Map<String, Object> viewing = findViewing(id, viewingId);
        if (isLocked(item) || !SalesInquiryUiData.isActive(viewing)) {
            flash.addFlashAttribute("flashError", "Lịch này đã kết thúc nên không đổi được.");
            return "redirect:" + BASE + "/" + id;
        }
        LocalDateTime start = LocalDateTime.parse((String) viewing.get("start"));
        Map<String, String> form = new HashMap<>();
        form.put("date", start.toLocalDate().toString());
        form.put("time", start.toLocalTime().toString());
        form.put("minutes", String.valueOf(viewing.get("minutes")));
        form.put("type", String.valueOf(viewing.get("typeKey")));
        form.put("place", String.valueOf(viewing.get("place")));
        form.put("note", String.valueOf(viewing.get("note")));
        return showViewing(item, viewing, form, null, model);
    }

    @PostMapping("/{id}/viewings/{viewingId}")
    public String reschedule(
            @PathVariable long id,
            @PathVariable long viewingId,
            @RequestParam Map<String, String> form,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        Map<String, Object> viewing = findViewing(id, viewingId);
        if (isLocked(item) || !SalesInquiryUiData.isActive(viewing)) {
            flash.addFlashAttribute("flashError", "Lịch này đã kết thúc nên không đổi được.");
            return "redirect:" + BASE + "/" + id;
        }
        String error = checkSchedule(item, form, viewingId);
        if (error != null) {
            return showViewing(item, viewing, form, error, model);
        }
        applySchedule(viewing, form);
        flash.addFlashAttribute("flashMessage", "Đã đổi lịch xem xe, cần xác nhận lại với khách.");
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/viewings/{viewingId}/confirm")
    public String confirmViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return moveViewing(id, viewingId, SalesInquiryUiData.CONFIRMED, "Đã xác nhận lịch xem xe.", flash);
    }

    @PostMapping("/{id}/viewings/{viewingId}/done")
    public String finishViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return moveViewing(id, viewingId, SalesInquiryUiData.DONE, "Đã ghi nhận khách đã xem xe.", flash);
    }

    @PostMapping("/{id}/viewings/{viewingId}/cancel")
    public String cancelViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return moveViewing(id, viewingId, SalesInquiryUiData.CANCELLED, "Đã hủy lịch xem xe.", flash);
    }

    private String moveViewing(long id, long viewingId, String next, String message, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        Map<String, Object> viewing = findViewing(id, viewingId);
        if (isLocked(item) || !SalesInquiryUiData.isActive(viewing)) {
            flash.addFlashAttribute("flashError", "Lịch này đã kết thúc.");
            return "redirect:" + BASE + "/" + id;
        }
        viewing.put("status", next);
        if (SalesInquiryUiData.DONE.equals(next)) {
            item.put("status", SalesInquiryUiData.VIEWED);
        } else if (SalesInquiryUiData.CANCELLED.equals(next) && !SalesInquiryUiData.hasActiveViewing(id)) {
            item.put("status", SalesInquiryUiData.statusWithoutActiveStep(id));
        }
        flash.addFlashAttribute("flashMessage", message);
        return "redirect:" + BASE + "/" + id;
    }

    private String showCreate(Map<String, String> form, String error, Model model) {
        var buyer = SalesInquiryUiData.buyerByPhone(form.get("phone"));
        buyer.ifPresent(b -> {
            form.put("fullName", String.valueOf(b.get("buyer")));
            form.put("email", String.valueOf(b.get("email")));
            form.put("area", String.valueOf(b.get("area")));
        });
        model.addAttribute("form", form);
        model.addAttribute("knownBuyer", buyer.isPresent());
        model.addAttribute("searched", form.containsKey("phone") && !form.get("phone").isBlank());
        model.addAttribute("cars", SalesDealUiData.availableCars());
        model.addAttribute("formError", error);
        return CREATE_VIEW;
    }

    private String showViewing(
            Map<String, Object> item,
            Map<String, Object> viewing,
            Map<String, String> form,
            String error,
            Model model) {
        model.addAttribute("item", item);
        model.addAttribute("viewing", viewing);
        model.addAttribute("form", form);
        model.addAttribute("formError", error);
        model.addAttribute("slots", SalesInquiryUiData.SLOTS);
        model.addAttribute("durations", DURATIONS);
        model.addAttribute("places", SalesInquiryUiData.PLACES);
        model.addAttribute("viewingTypes", SalesInquiryUiData.VIEWING_TYPES);
        model.addAttribute("today", LocalDate.now().toString());
        return VIEWING_VIEW;
    }

    private static String checkSchedule(Map<String, Object> item, Map<String, String> form, Long viewingId) {
        LocalDateTime start;
        try {
            start = LocalDate.parse(form.getOrDefault("date", "")).atTime(LocalTime.parse(form.getOrDefault("time", "")));
        } catch (DateTimeParseException ex) {
            return "Vui lòng chọn ngày và giờ xem xe.";
        }
        if (!start.isAfter(LocalDateTime.now())) {
            return "Giờ hẹn phải ở tương lai.";
        }
        LocalDateTime end = start.plusMinutes(minutes(form));
        if (SalesInquiryUiData.carBusy(String.valueOf(item.get("car")), start, end, viewingId)) {
            return "Xe đã có lịch khác trong khung giờ này, vui lòng chọn giờ khác.";
        }
        return null;
    }

    private static void applySchedule(Map<String, Object> viewing, Map<String, String> form) {
        LocalDateTime start = LocalDate.parse(form.get("date")).atTime(LocalTime.parse(form.get("time")));
        int minutes = minutes(form);
        String type = form.getOrDefault("type", "VIEWING");
        viewing.put("start", start.toString());
        viewing.put("minutes", minutes);
        viewing.put("end", start.plusMinutes(minutes).toLocalTime().toString());
        viewing.put("place", form.getOrDefault("place", SalesInquiryUiData.PLACES.get(0)));
        viewing.put("typeKey", type);
        viewing.put("type", SalesInquiryUiData.VIEWING_TYPES.getOrDefault(type, "Xem xe"));
        viewing.put("status", SalesInquiryUiData.BOOKED);
        viewing.put("note", form.getOrDefault("viewingNote", form.getOrDefault("note", "")).trim());
    }

    private static int minutes(Map<String, String> form) {
        try {
            int value = Integer.parseInt(form.getOrDefault("minutes", "45"));
            return DURATIONS.contains(value) ? value : 45;
        } catch (NumberFormatException ex) {
            return 45;
        }
    }

    private static void applyDetails(Map<String, Object> row, Map<String, String> form) {
        row.put("email", form.getOrDefault("email", "").trim());
        row.put("area", form.getOrDefault("area", "").trim());
        row.put("buyerNote", form.getOrDefault("buyerNote", "").trim());
        row.put("budgetMin", form.getOrDefault("budgetMin", "").trim());
        row.put("budgetMax", form.getOrDefault("budgetMax", "").trim());
        row.put("buyTime", form.getOrDefault("buyTime", "").trim());
        List<String> priorities = new ArrayList<>();
        Arrays.stream(form.getOrDefault("priorities", "").split(","))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .forEach(priorities::add);
        row.put("priorities", priorities);
        if (form.containsKey("note")) {
            row.put("note", form.get("note").trim());
        }
    }

    private static boolean isLocked(Map<String, Object> item) {
        return SalesInquiryUiData.CLOSED.equals(item.get("status"))
                || SalesInquiryUiData.CONVERTED.equals(item.get("status"));
    }

    private static String locked(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Tư vấn đã kết thúc nên không thay đổi được.");
        return "redirect:" + BASE + "/" + id;
    }

    private static String notClaimed(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Cần nhận tư vấn trước khi đặt lịch.");
        return "redirect:" + BASE + "/" + id;
    }

    private static Map<String, Object> find(long id) {
        return SalesInquiryUiData.inquiry(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu tư vấn", id));
    }

    private static Map<String, Object> findViewing(long id, long viewingId) {
        return SalesInquiryUiData.viewing(id, viewingId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch xem xe", viewingId));
    }

    private static String haystack(Map<String, Object> inquiry) {
        String phone = String.valueOf(inquiry.get("phone"));
        return (inquiry.get("code") + " " + inquiry.get("buyer") + " " + phone + " " + phone.replace(" ", "")
                + " " + inquiry.get("car")).toLowerCase(Locale.ROOT);
    }

    private static String createdDay(Map<String, Object> inquiry) {
        return String.valueOf(inquiry.get("created")).substring(0, 10);
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String isoDate(String value) {
        return value != null && value.matches("\\d{4}-\\d{2}-\\d{2}") ? value : "";
    }
}
