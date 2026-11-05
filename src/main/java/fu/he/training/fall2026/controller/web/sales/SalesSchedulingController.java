package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.inquiry.InquiryResponse;
import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.exception.SalesSchedulingException;
import fu.he.training.fall2026.service.SalesSchedulingService;
import fu.he.training.fall2026.service.InquiryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.format.annotation.DateTimeFormat;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/sales")
public class SalesSchedulingController {
    private final SalesSchedulingService schedulingService;
    private final InquiryService inquiryService;

    @GetMapping("/viewings")
    public String viewings(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status, @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) String q, @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        model.addAttribute("rows", (q == null || q.isBlank()) && (type == null || type.isBlank())
                ? schedulingService.viewings(date, status, salespersonId, page, size)
                : schedulingService.viewings(date, status, salespersonId, q, type, page, size));
        model.addAttribute("date", date);
        model.addAttribute("status", status);
        model.addAttribute("salespersonId", salespersonId);
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("salespeople", schedulingService.salespeople());
        return "sales/scheduling/viewings";
    }

    @GetMapping("/viewings/new")
    public String viewingForm(@RequestParam(required = false) Long inquiryId, Model model,
            RedirectAttributes redirect) {
        if (inquiryId == null) {
            redirect.addFlashAttribute("error", "Hãy tạo hoặc chọn một tư vấn trước khi đặt lịch xem xe.");
            return "redirect:/sales/inquiries";
        }
        InquiryResponse details = inquiryService.getDetails(inquiryId);
        ViewingFormRequest form = new ViewingFormRequest();
        form.setInquiryId(inquiryId);

        model.addAttribute("viewingForm", form);
        model.addAttribute("details", details);
        return renderViewingForm(form, model, true);
    }

    @PostMapping("/viewings")
    public String createViewing(@Valid @ModelAttribute("viewingForm") ViewingFormRequest viewingForm, BindingResult binding,
            Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return renderViewingForm(viewingForm, model, false);
        }
        try {
            schedulingService.scheduleViewingFromForm(viewingForm);
            redirect.addFlashAttribute("success", "Viewing scheduled successfully.");
            return "redirect:/sales/inquiries/" + viewingForm.getInquiryId();
        } catch (SalesSchedulingException ex) {
            binding.rejectValue("vehicleId".equals(ex.getField()) ? "inquiryId" : ex.getField(), "sales.invalid", ex.getMessage());
        } catch (ConcurrencyFailureException ex) {
            SalesFormErrors.rejectConcurrentChange(binding);
        }
        return renderViewingForm(viewingForm, model, false);
    }

    @GetMapping("/viewings/{id}/edit")
    public String editViewingForm(@PathVariable long id, Model model) {
        ViewingFormRequest form = schedulingService.formForViewing(id);
        model.addAttribute("viewingForm", form);
        model.addAttribute("viewingId", id);
        model.addAttribute("editing", true);
        return renderViewingForm(form, model, false);
    }

    @PostMapping("/viewings/{id}/edit")
    public String editViewing(@PathVariable long id,
            @Valid @ModelAttribute("viewingForm") ViewingFormRequest form, BindingResult binding,
            Model model, RedirectAttributes redirect) {
        model.addAttribute("viewingId", id);
        model.addAttribute("editing", true);
        if (!binding.hasErrors()) {
            try {
                schedulingService.updateViewing(id, form);
                redirect.addFlashAttribute("success", "Viewing updated successfully.");
                return "redirect:/sales/viewings";
            } catch (SalesSchedulingException ex) {
                if ("viewingId".equals(ex.getField())) throw ex;
                binding.rejectValue(ex.getField(), "sales.invalid", ex.getMessage());
            } catch (ConcurrencyFailureException ex) {
                SalesFormErrors.rejectConcurrentChange(binding);
            }
        }
        return renderViewingForm(form, model, false);
    }

    @GetMapping("/viewings/{id}/delete")
    public String deleteViewingConfirmation(@PathVariable long id, Model model) {
        model.addAttribute("viewing", schedulingService.viewingForDelete(id));
        model.addAttribute("revision", schedulingService.formForViewing(id).getRevision());
        return "sales/scheduling/viewing-delete";
    }

    @PostMapping("/viewings/{id}/delete")
    public String deleteViewing(@PathVariable long id, @RequestParam String revision,
            Model model, RedirectAttributes redirect) {
        try {
            schedulingService.deleteViewing(id, revision);
            redirect.addFlashAttribute("success", "Viewing deleted successfully.");
            return "redirect:/sales/viewings";
        } catch (SalesSchedulingException ex) {
            if ("viewingId".equals(ex.getField())) throw ex;
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("viewing", schedulingService.viewingForDelete(id));
            model.addAttribute("revision", revision);
            return "sales/scheduling/viewing-delete";
        }
    }

    private String renderViewingForm(ViewingFormRequest form, Model model, boolean selectCurrentSalesperson) {
        if (form.getInquiryId() != null && !model.containsAttribute("details")) {
            try {
                model.addAttribute("details", inquiryService.getDetails(form.getInquiryId()));
            } catch (SalesSchedulingException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        List<SalesOptionResponse> salespeople = schedulingService.assignableSalespeople();
        if (selectCurrentSalesperson && form.getSalespersonId() == null && !salespeople.isEmpty()) {
            form.setSalespersonId(salespeople.getFirst().getId());
        }
        if (model.containsAttribute("editing")) {
            long viewingId = (Long) model.asMap().get("viewingId");
            Long assignedId = schedulingService.formForViewing(viewingId).getSalespersonId();
            if (assignedId != null && salespeople.stream().noneMatch(o -> assignedId.equals(o.getId()))) {
                List<SalesOptionResponse> editable = new ArrayList<>(salespeople);
                editable.add(new SalesOptionResponse(assignedId,
                        schedulingService.viewingForDelete(viewingId).getSalesperson()));
                salespeople = editable;
            }
        }
        model.addAttribute("salespeople", salespeople);
        return "sales/scheduling/viewing-form";
    }
}
