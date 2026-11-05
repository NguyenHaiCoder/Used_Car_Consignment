package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.inquiry.*;
import fu.he.training.fall2026.dto.sales.SalesOptionResponse;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.exception.SalesSchedulingException;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.SalesTimeService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
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
@RequestMapping("/sales/inquiries")
public class InquiryWebController {
    private final InquiryService inquiryService;
    private final SalesTimeService time;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        model.addAttribute("rows", inquiryService.list(search, status, salespersonId, page, size));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("salespersonId", salespersonId);
        model.addAttribute("statuses", InquiryStatus.values());
        model.addAttribute("salespeople", inquiryService.salespeople());
        return "sales/inquiries/scheduling-list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long buyerId, Model model) {
        InquirySaveRequest form = new InquirySaveRequest();
        if (buyerId != null) form.setBuyerId(buyerId.toString());
        form.setInquiryDate(time.today());
        model.addAttribute("restoreDraft", true);
        model.addAttribute("inquiryForm", form);
        return renderInquiryForm(form, model);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("inquiryForm") InquirySaveRequest inquiryForm, BindingResult binding,
            @RequestParam(name = "next", required = false) String next, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return renderInquiryForm(inquiryForm, model);
        }
        try {
            long id = inquiryService.create(inquiryForm);
            redirect.addFlashAttribute("success", "Inquiry created successfully.");
            redirect.addFlashAttribute("inquiryCreated", true);
            if ("schedule".equals(next)) {
                return "redirect:/sales/viewings/new?inquiryId=" + id;
            }
            return "redirect:/sales/inquiries/" + id;
        } catch (SalesSchedulingException ex) {
            if ("salespersonId".equals(ex.getField())) {
                binding.reject("sales.invalid", ex.getMessage());
            } else {
                binding.rejectValue(ex.getField(), "sales.invalid", ex.getMessage());
            }
        } catch (ConcurrencyFailureException ex) {
            SalesFormErrors.rejectConcurrentChange(binding);
        }
        return renderInquiryForm(inquiryForm, model);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        return renderInquiryDetails(id, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        InquirySaveRequest form = inquiryService.formFor(id);
        model.addAttribute("inquiryForm", form);
        return renderEditInquiryForm(id, form, model);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id,
            @Valid @ModelAttribute("inquiryForm") InquirySaveRequest form, BindingResult binding,
            Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                inquiryService.update(id, form);
                redirect.addFlashAttribute("success", "Inquiry updated successfully.");
                redirect.addFlashAttribute("inquiryUpdated", true);
                return "redirect:/sales/inquiries/" + id;
            } catch (SalesSchedulingException ex) {
                if ("inquiryId".equals(ex.getField())) throw ex;
                binding.rejectValue(ex.getField(), "sales.invalid", ex.getMessage());
            } catch (ConcurrencyFailureException ex) {
                SalesFormErrors.rejectConcurrentChange(binding);
            }
        }
        return renderEditInquiryForm(id, form, model);
    }

    @PostMapping("/{id}/vehicles")
    public String reloadEditVehicles(@PathVariable long id,
            @ModelAttribute("inquiryForm") InquirySaveRequest form, BindingResult binding, Model model) {
        inquiryService.formFor(id); // Authorize the existing inquiry; retain submitted values.
        return renderEditInquiryForm(id, form, model);
    }

    @GetMapping("/{id}/delete")
    public String deleteConfirmation(@PathVariable long id, Model model) {
        InquiryDeleteResponse deletion = inquiryService.deletionFor(id);
        InquiryDeleteRequest form = new InquiryDeleteRequest();
        form.setRevision(deletion.getRevision());
        model.addAttribute("deleteForm", form);
        model.addAttribute("deletion", deletion);
        return "sales/inquiries/scheduling-delete";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id,
            @Valid @ModelAttribute("deleteForm") InquiryDeleteRequest form, BindingResult binding,
            Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                inquiryService.delete(id, form);
                redirect.addFlashAttribute("success", "Inquiry deleted successfully.");
                return "redirect:/sales/inquiries";
            } catch (SalesSchedulingException ex) {
                if ("inquiryId".equals(ex.getField())) throw ex;
                binding.rejectValue(ex.getField(), "sales.invalid", ex.getMessage());
            } catch (ConcurrencyFailureException ex) {
                SalesFormErrors.rejectConcurrentChange(binding);
            }
        }
        model.addAttribute("deletion", inquiryService.deletionFor(id));
        return "sales/inquiries/scheduling-delete";
    }

    @PostMapping("/vehicles")
    public String reloadVehicles(@ModelAttribute("inquiryForm") InquirySaveRequest form, BindingResult binding,
            Model model) {
        return renderInquiryForm(form, model);
    }

    private String renderInquiryDetails(long id, Model model) {
        model.addAttribute("details", inquiryService.getDetails(id));
        return "sales/inquiries/scheduling-detail";
    }

    private String renderInquiryForm(InquirySaveRequest form, Model model) {
        Long buyerId = form.selectedBuyerId();
        model.addAttribute("buyers", retainSelection(inquiryService.buyers(), buyerId, "Buyer"));
        model.addAttribute("vehicles", retainSelection(inquiryService.vehicles(buyerId),
                form.selectedVehicleId(), "Vehicle"));
        return "sales/inquiries/scheduling-form";
    }

    private String renderEditInquiryForm(long id, InquirySaveRequest form, Model model) {
        model.addAttribute("editing", true);
        model.addAttribute("inquiryId", id);
        return renderInquiryForm(form, model);
    }

    private List<SalesOptionResponse> retainSelection(List<SalesOptionResponse> options, Long id, String label) {
        if (id == null || options.stream().anyMatch(o -> id.equals(o.getId()))) return options;
        List<SalesOptionResponse> result = new ArrayList<>(options);
        result.add(new SalesOptionResponse(id, label + " #" + id + " (unavailable; select another)"));
        return result;
    }

}
