package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.constant.sales.SalesInquiryCatalog;
import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.sales.SalesInquiryCloseForm;
import fu.he.training.fall2026.dto.sales.SalesInquiryFilter;
import fu.he.training.fall2026.dto.sales.SalesInquiryForm;
import fu.he.training.fall2026.dto.sales.ViewingScheduleForm;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.service.SalesInquiryService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
 * Luồng Tư vấn &amp; lịch xem xe — Controller mỏng, bind form bằng DTO.
 */
@Controller
@RequestMapping("/sales/inquiries")
@RequiredArgsConstructor
public class SalesInquiryWebController {

    private static final String BASE = "/sales/inquiries";
    private static final String LIST_VIEW = "sales/inquiries/list";
    private static final String CREATE_VIEW = "sales/inquiries/create";
    private static final String DETAIL_VIEW = "sales/inquiries/detail";
    private static final String EDIT_VIEW = "sales/inquiries/edit";
    private static final String VIEWING_VIEW = "sales/inquiries/viewing";

    private final SalesInquiryService salesInquiryService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesInquiryCatalog.STATUSES);
        model.addAttribute("viewingNames", SalesInquiryCatalog.VIEWING_NAMES);
        model.addAttribute("viewingPills", SalesInquiryCatalog.VIEWING_PILLS);
        model.addAttribute("channels", SalesInquiryCatalog.CHANNELS);
        model.addAttribute("needs", SalesInquiryCatalog.NEEDS);
        model.addAttribute("dayNames", SalesInquiryCatalog.DAY_NAMES);
    }

    @RequirePermission(Perm.INQUIRY_VIEW)
    @GetMapping
    public String list(@ModelAttribute("filter") SalesInquiryFilter filter, Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(filter.sizeOrNull());
        filter.setSize(String.valueOf(pageSize));
        filter.setQ(blank(filter.getQ()));
        filter.setStatus(blank(filter.getStatus()));
        filter.setViewing(blank(filter.getViewing()));
        filter.setFrom(isoDate(filter.getFrom()));
        filter.setTo(isoDate(filter.getTo()));

        Page<Map<String, Object>> result = CatalogPageSupport.lastPageIfBeyond(
                search(filter, filter.pageOrZero(), pageSize),
                p -> search(filter, p, pageSize));

        List<Long> ids = result.getContent().stream().map(row -> (Long) row.get("id")).toList();
        Map<Long, Map<String, Object>> latest = salesInquiryService.latestViewings(ids);
        Map<String, Object> current = result.getContent().stream()
                .filter(i -> i.get("id").equals(filter.selectedOrNull()))
                .findFirst()
                .orElse(result.hasContent() ? result.getContent().get(0) : null);

        model.addAttribute("page", result);
        model.addAttribute("counts", salesInquiryService.countByStatus());
        model.addAttribute("q", filter.getQ());
        model.addAttribute("status", filter.getStatus());
        model.addAttribute("viewing", filter.getViewing());
        model.addAttribute("dateFrom", filter.getFrom());
        model.addAttribute("dateTo", filter.getTo());
        model.addAttribute("size", pageSize);
        model.addAttribute("viewings", latest);
        model.addAttribute("cur", current);
        model.addAttribute("curView", current == null ? null : latest.get(current.get("id")));
        return LIST_VIEW;
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String car, Model model) {
        SalesInquiryForm form = new SalesInquiryForm();
        if (car != null && !car.isBlank()) {
            form.setCar(car.trim());
        }
        return showCreate(form, null, model);
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") SalesInquiryForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return showCreate(form, null, model);
        }
        try {
            Long id = salesInquiryService.createByStaff(form, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage",
                    "Đã tạo tư vấn " + salesInquiryService.getCard(id).get("code") + ".");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            if (ex.getRelatedId() != null) {
                flash.addFlashAttribute("flashError", ex.getMessage());
                return "redirect:" + BASE + "/" + ex.getRelatedId();
            }
            rejectBusiness(errors, form, ex.getMessage());
            return showCreate(form, null, model);
        }
    }

    @RequirePermission(Perm.INQUIRY_VIEW)
    @GetMapping("/{id}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) String close,
            Model model) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        boolean locked = isLocked(item);
        model.addAttribute("item", item);
        model.addAttribute("viewingList", salesInquiryService.viewingsOf(id));
        model.addAttribute("closeReasons", SalesInquiryCatalog.CLOSE_REASONS);
        model.addAttribute("locked", locked);
        model.addAttribute("openClose", close != null && !locked);
        model.addAttribute("reservation", salesInquiryService.latestReservationCard(id).orElse(null));
        model.addAttribute("canHold", !locked && salesInquiryService.canHold(id));
        model.addAttribute("closeForm", new SalesInquiryCloseForm());
        return DETAIL_VIEW;
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        model.addAttribute("item", item);
        model.addAttribute("form", toEditForm(item));
        return EDIT_VIEW;
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @PostMapping("/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") SalesInquiryForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        if (errors.hasErrors()) {
            model.addAttribute("item", item);
            model.addAttribute("form", form);
            return EDIT_VIEW;
        }
        try {
            salesInquiryService.update(id, form);
            flash.addFlashAttribute("flashMessage",
                    "Đã cập nhật tư vấn " + salesInquiryService.getCard(id).get("code") + ".");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            rejectBusiness(errors, form, ex.getMessage());
            model.addAttribute("item", item);
            model.addAttribute("form", form);
            return EDIT_VIEW;
        }
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @PostMapping("/{id}/claim")
    public String claim(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        try {
            salesInquiryService.claim(id, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage",
                    "Đã nhận tư vấn " + salesInquiryService.getCard(id).get("code") + ".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @RequirePermission(Perm.INQUIRY_EDIT)
    @PostMapping("/{id}/notes")
    public String saveNote(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String note,
            RedirectAttributes flash) {
        try {
            salesInquiryService.saveNote(id, note);
            flash.addFlashAttribute("flashMessage", "Đã lưu ghi chú tư vấn.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @RequirePermission(Perm.INQUIRY_CLOSE)
    @PostMapping("/{id}/close")
    public String close(
            @PathVariable long id,
            @Valid @ModelAttribute("closeForm") SalesInquiryCloseForm closeForm,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            Map<String, Object> item = salesInquiryService.getCard(id);
            boolean locked = isLocked(item);
            model.addAttribute("item", item);
            model.addAttribute("viewingList", salesInquiryService.viewingsOf(id));
            model.addAttribute("closeReasons", SalesInquiryCatalog.CLOSE_REASONS);
            model.addAttribute("locked", locked);
            model.addAttribute("openClose", !locked);
            model.addAttribute("reservation", salesInquiryService.latestReservationCard(id).orElse(null));
            model.addAttribute("canHold", !locked && salesInquiryService.canHold(id));
            model.addAttribute("closeForm", closeForm);
            return DETAIL_VIEW;
        }
        try {
            salesInquiryService.close(id, closeForm.getCloseReason(), closeForm.getNote());
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @GetMapping("/{id}/viewings/new")
    public String viewingForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        if (isLocked(item)) {
            return locked(id, flash);
        }
        if ("NEW".equals(item.get("status"))) {
            return notClaimed(id, flash);
        }
        ViewingScheduleForm form = new ViewingScheduleForm();
        form.setDate(LocalDate.now().plusDays(1).toString());
        form.setTime("09:00");
        form.setMinutes(45);
        form.setType("TEST_DRIVE".equals(item.get("need")) ? "TEST_DRIVE" : "VIEWING");
        form.setPlace(SalesInquiryCatalog.PLACES.get(0));
        return showViewing(item, null, form, null, model);
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @PostMapping("/{id}/viewings")
    public String addViewing(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ViewingScheduleForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        if (errors.hasErrors()) {
            return showViewing(item, null, form, null, model);
        }
        try {
            salesInquiryService.scheduleViewing(
                    id, form.toStart(), form.safeMinutes(), form.getType(), form.getPlace(), form.getNote());
            flash.addFlashAttribute("flashMessage", "Đã đặt lịch xem xe cho " + item.get("buyer") + ".");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException | DateTimeParseException | NullPointerException ex) {
            String message = ex instanceof BusinessException be
                    ? be.getMessage()
                    : "Vui lòng chọn ngày và giờ xem xe.";
            errors.reject("error", message);
            return showViewing(item, null, form, null, model);
        }
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @GetMapping("/{id}/viewings/{viewingId}/edit")
    public String rescheduleForm(
            @PathVariable long id,
            @PathVariable long viewingId,
            Model model,
            RedirectAttributes flash) {
        try {
            Map<String, Object> item = salesInquiryService.getCard(id);
            Map<String, Object> viewing = salesInquiryService.getViewing(id, viewingId);
            if (isLocked(item) || !isActive(viewing)) {
                flash.addFlashAttribute("flashError", "Lịch này đã kết thúc nên không đổi được.");
                return "redirect:" + BASE + "/" + id;
            }
            LocalDateTime start = LocalDateTime.parse((String) viewing.get("start"));
            ViewingScheduleForm form = new ViewingScheduleForm();
            form.setDate(start.toLocalDate().toString());
            form.setTime(start.toLocalTime().toString());
            form.setMinutes((Integer) viewing.get("minutes"));
            form.setType(String.valueOf(viewing.get("typeKey")));
            form.setPlace(String.valueOf(viewing.get("place")));
            form.setNote(String.valueOf(viewing.get("note")));
            return showViewing(item, viewing, form, null, model);
        } catch (ResourceNotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/" + id;
        }
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @PostMapping("/{id}/viewings/{viewingId}")
    public String reschedule(
            @PathVariable long id,
            @PathVariable long viewingId,
            @Valid @ModelAttribute("form") ViewingScheduleForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        Map<String, Object> item = salesInquiryService.getCard(id);
        Map<String, Object> viewing = salesInquiryService.getViewing(id, viewingId);
        if (errors.hasErrors()) {
            return showViewing(item, viewing, form, null, model);
        }
        try {
            salesInquiryService.rescheduleViewing(
                    id, viewingId, form.toStart(), form.safeMinutes(), form.getType(), form.getPlace(), form.getNote());
            flash.addFlashAttribute("flashMessage", "Đã đổi lịch xem xe, cần xác nhận lại với khách.");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException | DateTimeParseException | NullPointerException ex) {
            String message = ex instanceof BusinessException be
                    ? be.getMessage()
                    : "Vui lòng chọn ngày và giờ xem xe.";
            errors.reject("error", message);
            return showViewing(item, viewing, form, null, model);
        }
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @PostMapping("/{id}/viewings/{viewingId}/confirm")
    public String confirmViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return runViewingAction(id, () -> salesInquiryService.confirmViewing(id, viewingId),
                "Đã xác nhận lịch xem xe.", flash);
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @PostMapping("/{id}/viewings/{viewingId}/done")
    public String finishViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return runViewingAction(id, () -> salesInquiryService.finishViewing(id, viewingId),
                "Đã ghi nhận khách đã xem xe.", flash);
    }

    @RequirePermission(Perm.VIEWING_MANAGE)
    @PostMapping("/{id}/viewings/{viewingId}/cancel")
    public String cancelViewing(@PathVariable long id, @PathVariable long viewingId, RedirectAttributes flash) {
        return runViewingAction(id, () -> salesInquiryService.cancelViewing(id, viewingId),
                "Đã hủy lịch xem xe.", flash);
    }

    private Page<Map<String, Object>> search(SalesInquiryFilter filter, int page, int pageSize) {
        return salesInquiryService.search(
                filter.getQ(),
                parseStatus(filter.getStatus()),
                parseViewing(filter.getViewing()),
                null,
                null,
                parseDate(filter.getFrom()),
                parseDate(filter.getTo()),
                PageRequest.of(page, pageSize));
    }

    private String runViewingAction(long id, Runnable action, String okMessage, RedirectAttributes flash) {
        try {
            action.run();
            flash.addFlashAttribute("flashMessage", okMessage);
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    private String showCreate(SalesInquiryForm form, String error, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("cars", salesInquiryService.listedCars());
        model.addAttribute("formError", error);
        return CREATE_VIEW;
    }

    /** Gắn lỗi nghiệp vụ vào đúng field (SĐT / xe) hoặc lỗi chung. */
    private static void rejectBusiness(BindingResult errors, SalesInquiryForm form, String message) {
        if (message == null || message.isBlank()) {
            errors.reject("error", "Không lưu được tư vấn.");
            return;
        }
        String lower = message.toLowerCase();
        if (lower.contains("điện thoại") || lower.contains("sđt")) {
            errors.rejectValue("phone", "phone", message);
        } else if (lower.contains("xe")) {
            errors.rejectValue("car", "car", message);
        } else if (lower.contains("họ") || lower.contains("tên")) {
            errors.rejectValue("fullName", "fullName", message);
        } else if (lower.contains("đồng ý") || lower.contains("consent")) {
            errors.rejectValue("consentAccepted", "consentAccepted", message);
        } else {
            errors.reject("error", message);
        }
    }

    private String showViewing(
            Map<String, Object> item,
            Map<String, Object> viewing,
            ViewingScheduleForm form,
            String error,
            Model model) {
        model.addAttribute("item", item);
        model.addAttribute("viewing", viewing);
        model.addAttribute("form", form);
        model.addAttribute("formError", error);
        model.addAttribute("slots", SalesInquiryCatalog.SLOTS);
        model.addAttribute("durations", SalesInquiryCatalog.DURATIONS);
        model.addAttribute("places", SalesInquiryCatalog.PLACES);
        model.addAttribute("viewingTypes", SalesInquiryCatalog.VIEWING_TYPES);
        model.addAttribute("today", LocalDate.now().toString());
        return VIEWING_VIEW;
    }

    private static SalesInquiryForm toEditForm(Map<String, Object> item) {
        SalesInquiryForm form = new SalesInquiryForm();
        form.setFullName(String.valueOf(item.getOrDefault("buyer", "")));
        form.setPhone(String.valueOf(item.getOrDefault("phone", "")));
        form.setEmail(String.valueOf(item.getOrDefault("email", "")));
        form.setArea(String.valueOf(item.getOrDefault("area", "")));
        form.setBuyerNote(String.valueOf(item.getOrDefault("buyerNote", "")));
        form.setCar(String.valueOf(item.getOrDefault("vehicleId", "")));
        form.setNeed(String.valueOf(item.getOrDefault("need", "PRICE")));
        form.setNote(String.valueOf(item.getOrDefault("note", "")));
        form.setBudgetMin(String.valueOf(item.getOrDefault("budgetMin", "")));
        form.setBudgetMax(String.valueOf(item.getOrDefault("budgetMax", "")));
        form.setBuyTime(String.valueOf(item.getOrDefault("buyTime", "")));
        form.setConsent(true);
        Object priorities = item.get("priorities");
        if (priorities instanceof List<?> list) {
            form.setPriorities(String.join(", ", list.stream().map(String::valueOf).toList()));
        }
        return form;
    }

    private static boolean isLocked(Map<String, Object> item) {
        String status = String.valueOf(item.get("status"));
        return "CLOSED".equals(status) || "CONVERTED".equals(status);
    }

    private static boolean isActive(Map<String, Object> viewing) {
        String status = String.valueOf(viewing.get("status"));
        return "BOOKED".equals(status) || "CONFIRMED".equals(status);
    }

    private static String locked(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Tư vấn đã kết thúc nên không thay đổi được.");
        return "redirect:" + BASE + "/" + id;
    }

    private static String notClaimed(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Cần nhận tư vấn trước khi đặt lịch.");
        return "redirect:" + BASE + "/" + id;
    }

    private static InquiryStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return InquiryStatus.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ViewingStatus parseViewing(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ViewingStatus.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        String iso = isoDate(value);
        return iso.isEmpty() ? null : LocalDate.parse(iso);
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String isoDate(String value) {
        return value != null && value.matches("\\d{4}-\\d{2}-\\d{2}") ? value : "";
    }
}
