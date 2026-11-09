package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.constant.sales.SalesDealCatalog;
import fu.he.training.fall2026.dto.sales.CancelReservationForm;
import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationForm;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.dto.sales.SalesContractResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.service.ReservationService;
import fu.he.training.fall2026.service.SalesContractService;
import fu.he.training.fall2026.service.SalesInquiryService;
import fu.he.training.fall2026.service.impl.ReservationServiceImpl;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

/** Đặt cọc giữ xe — controller mỏng, dữ liệu lấy từ {@link ReservationService}. */
@Controller
@RequestMapping("/sales/reservations")
@RequiredArgsConstructor
public class SalesReservationWebController {

    private static final String BASE = "/sales/reservations";
    private static final String LIST_VIEW = "sales/reservations/list";
    private static final String FORM_VIEW = "sales/reservations/form";
    private static final String DETAIL_VIEW = "sales/reservations/detail";
    private static final int DEFAULT_HOLD_DAYS = 7;

    private final ReservationService reservationService;
    private final SalesContractService salesContractService;
    private final SalesInquiryService salesInquiryService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesDealCatalog.HOLD_STATUSES);
        model.addAttribute("decisions", SalesDealCatalog.DEPOSIT_DECISIONS);
    }

    @RequirePermission(Perm.RESERVATION_VIEW)
    @GetMapping
    public String list(@ModelAttribute DealListFilter filter, Model model) {
        reservationService.bindList(model, BASE, filter);
        return LIST_VIEW;
    }

    @RequirePermission(Perm.RESERVATION_CREATE)
    @GetMapping("/new")
    public String createForm(
            @RequestParam Map<String, String> params,
            Model model,
            RedirectAttributes flash) {
        ReservationForm form = new ReservationForm();
        form.setCar(blank(params.get("car")));
        form.setInquiryId(blank(params.get("inquiryId")));
        form.setDeposit(blank(params.get("deposit")));
        form.setHoldUntil(blank(params.get("holdUntil")));
        form.setTerms(blank(params.get("terms")));
        if (params.containsKey("consent")) {
            form.setConsent(params.get("consent"));
        }

        if (!form.getInquiryId().isBlank()) {
            Map<String, Object> inquiry = findInquiry(form.getInquiryId());
            String error = holdError(inquiry);
            if (error != null) {
                flash.addFlashAttribute("flashError", error);
                return inquiry == null
                        ? "redirect:" + BASE
                        : "redirect:/sales/inquiries/" + inquiry.get("id");
            }
            form.setCar(String.valueOf(inquiry.get("vehicleId")));
            form.setInquiryId(String.valueOf(inquiry.get("id")));
        }

        if (form.getHoldUntil().isBlank()) {
            form.setHoldUntil(LocalDate.now().plusDays(DEFAULT_HOLD_DAYS).toString());
        }
        if (form.getTerms().isBlank()) {
            form.setTerms(SalesDealCatalog.HOLD_TERMS);
        }
        return showCreate(form, null, model);
    }

    @RequirePermission(Perm.RESERVATION_CREATE)
    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ReservationForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return showCreate(form, null, model);
        }
        try {
            Long id = reservationService.create(form, SessionAuthUtils.getUserId(session));
            ReservationResponse saved = reservationService.findById(id);
            flash.addFlashAttribute(
                    "flashMessage",
                    "Đã tạo phiếu đặt cọc " + saved.getCode() + ", có thể lập hợp đồng bán xe.");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            rejectReservationBusiness(errors, ex.getMessage());
            return showCreate(form, null, model);
        }
    }

    @RequirePermission(Perm.RESERVATION_VIEW)
    @GetMapping("/{id}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) String cancel,
            Model model) {
        ReservationResponse item = reservationService.findById(id);
        boolean canDecide = "ACTIVE".equals(item.getStatus()) || "EXPIRED".equals(item.getStatus());
        SalesContractResponse contract = salesContractService.findByReservationId(id).orElse(null);
        model.addAttribute("item", item);
        model.addAttribute("contract", contract);
        model.addAttribute("canDecide", canDecide);
        model.addAttribute("openDialog", cancel != null && canDecide);
        model.addAttribute("cancelForm", new CancelReservationForm());
        return DETAIL_VIEW;
    }

    @RequirePermission(Perm.RESERVATION_EDIT)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model, RedirectAttributes flash) {
        ReservationResponse item = reservationService.findById(id);
        if (!"ACTIVE".equals(item.getStatus())) {
            return ended(id, flash);
        }
        ReservationForm form = new ReservationForm();
        form.setCar(String.valueOf(item.getVehicleId()));
        form.setInquiryId(String.valueOf(item.getInquiryId()));
        form.setDeposit(ReservationServiceImpl.money(item.getDeposit()));
        form.setHoldUntil(item.getHoldUntil());
        form.setTerms(item.getTerms());
        form.setConsent("true");
        return showEdit(item, form, null, model);
    }

    @RequirePermission(Perm.RESERVATION_EDIT)
    @PostMapping("/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ReservationForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        ReservationResponse item = reservationService.findById(id);
        if (!"ACTIVE".equals(item.getStatus())) {
            return ended(id, flash);
        }
        if (errors.hasErrors()) {
            return showEdit(item, form, null, model);
        }
        try {
            reservationService.update(id, form);
            flash.addFlashAttribute("flashMessage", "Đã cập nhật phiếu đặt cọc " + item.getCode() + ".");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            rejectReservationBusiness(errors, ex.getMessage());
            return showEdit(item, form, null, model);
        }
    }

    @RequirePermission(Perm.RESERVATION_CANCEL)
    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable long id,
            @Valid @ModelAttribute("cancelForm") CancelReservationForm cancelForm,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        ReservationResponse item = reservationService.findById(id);
        boolean canDecide = "ACTIVE".equals(item.getStatus()) || "EXPIRED".equals(item.getStatus());
        if (!canDecide) {
            return ended(id, flash);
        }
        if (errors.hasErrors()) {
            model.addAttribute("item", item);
            model.addAttribute("contract", salesContractService.findByReservationId(id).orElse(null));
            model.addAttribute("canDecide", true);
            model.addAttribute("openDialog", true);
            model.addAttribute("cancelForm", cancelForm);
            return DETAIL_VIEW;
        }
        try {
            reservationService.cancel(id, cancelForm.getReason(), cancelForm.getDecision());
            if ("ACTIVE".equals(item.getStatus())) {
                flash.addFlashAttribute(
                        "flashMessage",
                        "Đã hủy phiếu đặt cọc " + item.getCode() + ", xe được mở bán lại.");
            } else {
                flash.addFlashAttribute(
                        "flashMessage",
                        "Đã ghi nhận cách xử lý tiền cọc của phiếu " + item.getCode() + ".");
            }
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    private String showCreate(ReservationForm form, String error, Model model) {
        Map<String, Map<String, Object>> cars = reservationService.availableCars();
        Map<String, Object> picked = cars.get(form.getCar());
        if (picked != null && form.getDeposit().isBlank()) {
            long price = ((Number) picked.get("price")).longValue();
            form.setDeposit(ReservationServiceImpl.money(Math.max(price / 10 / 1_000_000 * 1_000_000, 1_000_000)));
        }

        List<ReservationResponse> buyers = List.of();
        if (!form.getInquiryId().isBlank()) {
            Map<String, Object> inquiry = findInquiry(form.getInquiryId());
            if (inquiry != null) {
                ReservationResponse buyer = new ReservationResponse();
                buyer.setId((Long) inquiry.get("id"));
                buyer.setCode(String.valueOf(inquiry.get("code")));
                buyer.setBuyer(String.valueOf(inquiry.get("buyer")));
                buyer.setPhone(String.valueOf(inquiry.get("phone")));
                buyers = List.of(buyer);
            }
        } else if (picked != null) {
            buyers = reservationService.holdableBuyers(ReservationServiceImpl.parseId(form.getCar()));
        }

        model.addAttribute("cars", cars);
        model.addAttribute("picked", picked);
        model.addAttribute("carTitle", picked == null ? "" : String.valueOf(picked.get("title")));
        model.addAttribute("buyers", buyers);
        return showForm(null, form, error, model);
    }

    private String showEdit(
            ReservationResponse item, ReservationForm form, String error, Model model) {
        Map<String, Object> picked = new HashMap<>();
        picked.put("price", item.getPrice());
        picked.put("km", item.getKm());
        picked.put("fuel", item.getFuel());
        picked.put("color", item.getColor());
        picked.put("file", item.getFile());
        picked.put("plate", item.getPlate());
        picked.put("owner", item.getOwner());
        picked.put("vin", item.getVin());
        picked.put("title", item.getCar());
        model.addAttribute("picked", picked);
        model.addAttribute("carTitle", item.getCar());
        return showForm(item, form, error, model);
    }

    private String showForm(
            ReservationResponse item, ReservationForm form, String error, Model model) {
        model.addAttribute("item", item);
        model.addAttribute("form", form);
        model.addAttribute("formError", error);
        model.addAttribute("today", LocalDate.now().toString());
        return FORM_VIEW;
    }

    private static void rejectReservationBusiness(BindingResult errors, String message) {
        if (message == null || message.isBlank()) {
            errors.reject("error", "Không lưu được phiếu đặt cọc.");
            return;
        }
        String lower = message.toLowerCase();
        if (lower.contains("tiền cọc") || lower.contains("cọc")) {
            errors.rejectValue("deposit", "deposit", message);
        } else if (lower.contains("hạn giữ") || lower.contains("hold")) {
            errors.rejectValue("holdUntil", "holdUntil", message);
        } else if (lower.contains("xe")) {
            errors.rejectValue("car", "car", message);
        } else if (lower.contains("khách") || lower.contains("tư vấn")) {
            errors.rejectValue("inquiryId", "inquiryId", message);
        } else if (lower.contains("đồng ý")) {
            errors.rejectValue("consentAccepted", "consentAccepted", message);
        } else {
            errors.reject("error", message);
        }
    }

    private Map<String, Object> findInquiry(String id) {
        Long inquiryId = ReservationServiceImpl.parseId(id);
        if (inquiryId == null) {
            return null;
        }
        try {
            return salesInquiryService.getCard(inquiryId);
        } catch (ResourceNotFoundException | BusinessException ex) {
            return null;
        }
    }

    private String holdError(Map<String, Object> inquiry) {
        if (inquiry == null) {
            return "Không tìm thấy tư vấn.";
        }
        if ("NEW".equals(inquiry.get("status"))) {
            return "Cần nhận tư vấn trước khi đặt cọc.";
        }
        String status = String.valueOf(inquiry.get("status"));
        if (!List.of("OPEN", "SCHEDULED", "VIEWED").contains(status)) {
            return "Tư vấn này đã kết thúc nên không đặt cọc được.";
        }
        Long inquiryId = (Long) inquiry.get("id");
        if (!salesInquiryService.canHold(inquiryId)) {
            return "Tư vấn này đã có phiếu đặt cọc đang hiệu lực.";
        }
        Long vehicleId = (Long) inquiry.get("vehicleId");
        Map<String, Map<String, Object>> cars = reservationService.availableCars();
        if (vehicleId == null || !cars.containsKey(String.valueOf(vehicleId))) {
            return "Xe " + inquiry.get("car") + " đã được giữ hoặc đã bán.";
        }
        return null;
    }

    private static String ended(long id, RedirectAttributes flash) {
        flash.addFlashAttribute("flashError", "Phiếu đặt cọc này đã kết thúc.");
        return "redirect:" + BASE + "/" + id;
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }
}
