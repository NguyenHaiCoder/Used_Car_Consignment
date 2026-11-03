package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.buyer.BuyerResponse;
import fu.he.training.fall2026.dto.buyer.BuyerRequest;
import fu.he.training.fall2026.dto.buyer.DeleteBuyerRequest;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.service.BuyerService;
import fu.he.training.fall2026.validation.BuyerValidation;
import jakarta.validation.Valid;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/sales/buyers")
public class BuyerWebController {
    private final BuyerService buyerService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "desc") String idSort,
                       @RequestParam(defaultValue = "all") String status,
                       @RequestParam(defaultValue = "default") String createdSort, Model model) {
        model.addAttribute("buyers", buyerService.list(page, size, search, idSort, status, createdSort));
        model.addAttribute("search", search);
        model.addAttribute("idSort", idSort);
        model.addAttribute("status", status);
        model.addAttribute("createdSort", createdSort);
        return "sales/buyers/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size, Model model) {
        model.addAttribute("buyer", buyerService.getDetails(id));
        model.addAttribute("history", buyerService.getInquiryHistory(id, page, size));
        return "sales/buyers/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BuyerRequest());
        model.addAttribute("buyerId", null);
        model.addAttribute("editing", false);
        return "sales/buyers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("form", buyerService.formFor(id));
        model.addAttribute("buyerId", id);
        model.addAttribute("editing", true);
        return "sales/buyers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BuyerRequest input, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        validateFields(input, binding, false);
        if (binding.hasErrors()) {
            model.addAttribute("buyerId", null);
            model.addAttribute("editing", false);
            return "sales/buyers/form";
        }
        try {
            BuyerResponse buyer = buyerService.create(input);
            redirect.addFlashAttribute("flashMessage", "Buyer created successfully.");
            return "redirect:/sales/buyers/" + buyer.getBuyerId();
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            rejectBuyerError(binding, ex);
            model.addAttribute("buyerId", null);
            model.addAttribute("editing", false);
            return "sales/buyers/form";
        } catch (Exception ex) {
            binding.reject("buyer.persistence", "Không thể lưu Buyer lúc này. Vui lòng kiểm tra thông tin và thử lại.");
            model.addAttribute("buyerId", null);
            model.addAttribute("editing", false);
            return "sales/buyers/form";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") BuyerRequest input,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        validateFields(input, binding, true);
        if (binding.hasErrors()) {
            model.addAttribute("buyerId", id);
            model.addAttribute("editing", true);
            return "sales/buyers/form";
        }
        try {
            buyerService.update(id, input);
            redirect.addFlashAttribute("flashMessage", "Buyer updated successfully.");
            return "redirect:/sales/buyers/" + id;
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            if (ex.getReason() == BuyerException.Reason.CONFLICT && ex.getErrors().isEmpty()) {
                model.addAttribute("revisionConflict", true);
            }
            rejectBuyerError(binding, ex);
            model.addAttribute("buyerId", id);
            model.addAttribute("editing", true);
            return "sales/buyers/form";
        } catch (Exception ex) {
            binding.reject("buyer.persistence", "Không thể lưu Buyer lúc này. Vui lòng kiểm tra thông tin và thử lại.");
            model.addAttribute("buyerId", id);
            model.addAttribute("editing", true);
            return "sales/buyers/form";
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteConfirmation(@PathVariable long id, Model model) {
        model.addAttribute("deleteForm", new DeleteBuyerRequest());
        prepareDeleteModel(id, model);
        return "sales/buyers/details";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, @Valid @ModelAttribute("deleteForm") DeleteBuyerRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            prepareDeleteModel(id, model);
            return "sales/buyers/details";
        }
        try {
            buyerService.delete(id, form.getReason());
            redirect.addFlashAttribute("flashMessage", "Buyer deleted successfully.");
            return "redirect:/sales/buyers";
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            if (ex.getReason() == BuyerException.Reason.VALIDATION) {
                binding.rejectValue("reason", "buyer.invalid", ex.getMessage());
            } else {
                model.addAttribute("error", ex.getMessage());
            }
            prepareDeleteModel(id, model);
            return "sales/buyers/details";
        } catch (Exception ex) {
            model.addAttribute("error", "Không thể xóa Buyer lúc này. Vui lòng tải lại trang và thử lại.");
            prepareDeleteModel(id, model);
            return "sales/buyers/details";
        }
    }

    private void validateFields(BuyerRequest input, BindingResult binding, boolean editing) {
        for (Map.Entry<String, String> error : BuyerValidation.errors(
                input.getFullName(), input.getPhone(), input.getEmail(), input.getAddress()).entrySet()) {
            if (!binding.hasFieldErrors(error.getKey())) {
                binding.rejectValue(error.getKey(), "buyer.invalid", error.getValue());
            }
        }
        if (!editing && !binding.hasFieldErrors("consentStored") && !input.isConsentStored()) {
            binding.rejectValue("consentStored", "buyer.invalid", BuyerValidation.CONSENT_REQUIRED);
        }
    }

    private void rejectBuyerError(BindingResult binding, BuyerException ex) {
        if (ex.getErrors().isEmpty()) {
            binding.reject("buyer.business", ex.getMessage());
        } else {
            ex.getErrors().forEach((field, message) -> binding.rejectValue(field, "buyer.invalid", message));
        }
    }

    private void prepareDeleteModel(long id, Model model) {
        model.addAttribute("buyer", buyerService.getDetails(id));
        model.addAttribute("history", buyerService.getInquiryHistory(id, 1, 10));
        model.addAttribute("confirmDelete", true);
    }
}
