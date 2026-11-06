package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.sales.ReservationRequest;
import fu.he.training.fall2026.dto.sales.ReservationEditRequest;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.dto.sales.ReservationEligibilityResponse;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ReservationService;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.SalesPipelineViewService;
import fu.he.training.fall2026.service.SalesSchedulingService;
import fu.he.training.fall2026.service.SalesTimeService;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping({"/sales/reservations", "/reservations"})
public class ReservationController {
    private static final String DETAIL_REDIRECT = "redirect:/sales/reservations/";

    private final ReservationService reservationService;
    private final InquiryService inquiryService;
    private final SalesPipelineViewService viewService;
    private final SalesSchedulingService schedulingService;

    @GetMapping
    public String list(@RequestParam(required = false) ReservationStatus status,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Long vehicleId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holdUntil,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "desc") String sort, Model model) {
        int pageSize = size == 20 || size == 50 ? size : 10;
        String order = "asc".equalsIgnoreCase(sort) ? "asc" : "desc";
        var rows = viewService.reservations(status, q, vehicleId, holdUntil, page, pageSize, order);
        model.addAttribute("rows", rows);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ReservationStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("vehicleId", vehicleId);
        model.addAttribute("holdUntil", holdUntil);
        model.addAttribute("size", pageSize);
        model.addAttribute("sort", order);
        model.addAttribute("vehicleOptions", viewService.reservationVehicles());
        model.addAttribute("firstRow", rows.isEmpty() ? 0 : rows.getNumber() * pageSize + 1);
        model.addAttribute("lastRow", Math.min(rows.getTotalElements(), (long) (rows.getNumber() + 1) * pageSize));
        return "sales/reservations/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long inquiryId, Model model) {
        try {
            model.addAttribute("form", viewService.reservationForm(inquiryId));
        } catch (BusinessException | DataAccessException exception) {
            model.addAttribute("form", viewService.reservationForm(null));
            model.addAttribute("sourceInquiryError", "Phiếu tư vấn đã chọn không còn khả dụng.");
            model.addAttribute("eligibilityError", true);
        }
        return renderForm(model);
    }

    @GetMapping("/eligibility")
    @ResponseBody
    public ReservationEligibilityResponse eligibility(@RequestParam Long inquiryId) {
        return reservationService.eligibility(inquiryId);
    }

    @PostMapping
    public String create(@Validated(ReservationRequest.Create.class) @ModelAttribute("form") ReservationRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("showForm", true);
            return renderForm(model);
        }
        try {
            ReservationResponse reservation = reservationService.create(form);
            redirect.addFlashAttribute("success", "Đã tạo đơn giữ xe. Tiền cọc mới là khoản thỏa thuận, chưa xác nhận thu tiền.");
            return DETAIL_REDIRECT + reservation.getId();
        } catch (BusinessException | ConcurrencyFailureException | DataIntegrityViolationException exception) {
            SalesFormErrors.reject(binding, exception);
            model.addAttribute("showForm", true);
            return renderForm(model);
        }
    }

    @GetMapping({"/{id}", "/{id}/deposit-policy"})
    public String details(@PathVariable Long id, Model model) {
        return renderDetails(id, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", reservationService.editForm(id));
        return renderEdit(id, model);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Validated @ModelAttribute("form") ReservationEditRequest form,
                       BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return renderEdit(id, model);
        try {
            reservationService.update(id, form);
            redirect.addFlashAttribute("success", "Đã cập nhật thỏa thuận giữ xe và ghi nhận lịch sử thay đổi.");
            return DETAIL_REDIRECT + id;
        } catch (BusinessException | ConcurrencyFailureException | DataIntegrityViolationException exception) {
            SalesFormErrors.reject(binding, exception);
            return renderEdit(id, model);
        }
    }

    @GetMapping("/{id}/cancel")
    public String cancelForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", new ReservationRequest(null, "", null));
        return renderCancel(id, model);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @Validated(ReservationRequest.Finish.class) @ModelAttribute("form") ReservationRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return renderCancel(id, model);
        try {
            reservationService.cancel(id, form);
            redirect.addFlashAttribute("success", "Đã ghi nhận quyết định hủy. Chờ WP05 xác nhận xử lý tiền; xe vẫn được giữ.");
            return DETAIL_REDIRECT + id;
        } catch (BusinessException | ConcurrencyFailureException exception) {
            SalesFormErrors.reject(binding, exception);
            return renderCancel(id, model);
        }
    }

    @PostMapping("/{id}/expire")
    public String expire(@PathVariable Long id, @Validated(ReservationRequest.Finish.class) @ModelAttribute("form") ReservationRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return renderCancel(id, model);
        try {
            reservationService.expire(id, form);
            redirect.addFlashAttribute("success", "Đã ghi nhận quyết định hết hạn. Chờ WP05 xác nhận xử lý tiền; xe vẫn được giữ.");
            return DETAIL_REDIRECT + id;
        } catch (BusinessException | ConcurrencyFailureException exception) {
            SalesFormErrors.reject(binding, exception);
            return renderCancel(id, model);
        }
    }

    private String renderForm(Model model) {
        model.addAttribute("inquiries", viewService.inquiries());
        model.addAttribute("salespeople", schedulingService.salespeople());
        ReservationRequest form = (ReservationRequest) model.getAttribute("form");
        if (form != null && form.getInquiryId() != null) {
            try {
                model.addAttribute("sourceInquiry", inquiryService.getDetails(form.getInquiryId()));
            } catch (BusinessException | DataAccessException exception) {
                model.addAttribute("sourceInquiryError", "Phiếu tư vấn đã chọn không còn khả dụng.");
            }
            try {
                model.addAttribute("eligibility", reservationService.eligibility(form.getInquiryId()));
            } catch (BusinessException | DataAccessException exception) {
                model.addAttribute("eligibilityError", true);
            }
        }
        return "sales/reservations/create";
    }

    private String renderDetails(Long id, Model model) {
        model.addAttribute("reservation", viewService.reservation(id));
        model.addAttribute("history", viewService.reservationHistory(id));
        model.addAttribute("reservationTimeZone", SalesTimeService.ZONE);
        model.addAttribute("policy", reservationService.depositPolicy(id));
        model.addAttribute("contractId", viewService.contractId(id));
        model.addAttribute("targets", viewService.transferTargets(id));
        return "sales/reservations/detail";
    }

    private String renderEdit(Long id, Model model) {
        ReservationResponse reservation = viewService.reservation(id);
        model.addAttribute("reservation", reservation);
        model.addAttribute("financialTermsLocked", reservation.getConfirmedCollectedAmount() != null
            || reservation.getProcessedLiabilityAmount() != null
            || reservation.getDepositBalanceReference() != null);
        return "sales/reservations/edit";
    }

    private String renderCancel(Long id, Model model) {
        model.addAttribute("reservation", viewService.reservation(id));
        model.addAttribute("history", viewService.reservationHistory(id));
        model.addAttribute("policy", reservationService.depositPolicy(id));
        model.addAttribute("targets", viewService.transferTargets(id));
        return "sales/reservations/cancel";
    }
}
