package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.buyer.BuyerResponse;
import fu.he.training.fall2026.dto.buyer.BuyerRequest;
import fu.he.training.fall2026.dto.buyer.DeleteBuyerRequest;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.service.BuyerService;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.validation.BuyerFormValidation;
import jakarta.validation.Valid;
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
    private final InquiryService inquiryService;

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
        model.addAttribute("history", inquiryService.getBuyerHistory(id, page, size));
        return "sales/buyers/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BuyerRequest());
        return showForm(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("form", buyerService.formFor(id));
        return showForm(model, id);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BuyerRequest input, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        BuyerFormValidation.validateForCreate(input, binding);
        if (binding.hasErrors()) {
            return showForm(model, null);
        }
        try {
            BuyerResponse buyer = buyerService.create(input);
            redirect.addFlashAttribute("flashMessage", "Đã tạo khách mua thành công.");
            return "redirect:/sales/buyers/" + buyer.getBuyerId();
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            rejectBuyerError(binding, ex);
            return showForm(model, null);
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") BuyerRequest input,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        BuyerFormValidation.validateForUpdate(input, binding);
        if (binding.hasErrors()) {
            return showForm(model, id);
        }
        try {
            buyerService.update(id, input);
            redirect.addFlashAttribute("flashMessage", "Đã cập nhật khách mua thành công.");
            return "redirect:/sales/buyers/" + id;
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            if (ex.getReason() == BuyerException.Reason.CONFLICT && ex.getErrors().isEmpty()) {
                model.addAttribute("revisionConflict", true);
            }
            rejectBuyerError(binding, ex);
            return showForm(model, id);
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
            redirect.addFlashAttribute("flashMessage", "Đã xóa khách mua thành công.");
            return "redirect:/sales/buyers";
        } catch (BuyerException ex) {
            if (ex.getReason() == BuyerException.Reason.NOT_FOUND) {
                throw ex;
            }
            if (ex.getReason() == BuyerException.Reason.VALIDATION) {
                binding.rejectValue("reason", "buyer.invalid", ex.getMessage());
            }
            if (ex.getReason() == BuyerException.Reason.CONFLICT) {
                prepareDeleteModel(id, model, ex.getMessage());
            } else {
                prepareDeleteModel(id, model);
            }
            return "sales/buyers/details";
        }
    }

    private String showForm(Model model, Long buyerId) {
        model.addAttribute("buyerId", buyerId);
        model.addAttribute("editing", buyerId != null);
        return "sales/buyers/form";
    }

    private void rejectBuyerError(BindingResult binding, BuyerException ex) {
        if (ex.getErrors().isEmpty()) {
            binding.reject("buyer.business", ex.getMessage());
        } else {
            ex.getErrors().forEach((field, message) -> binding.rejectValue(field, "buyer.invalid", message));
        }
    }

    private void prepareDeleteModel(long id, Model model) {
        prepareDeleteModel(id, model, buyerService.deletionBlockReason(id));
    }

    private void prepareDeleteModel(long id, Model model, String blockedReason) {
        model.addAttribute("buyer", buyerService.getDetails(id));
        model.addAttribute("history", inquiryService.getBuyerHistory(id, 1, 10));
        model.addAttribute("deleteBlockedReason", blockedReason);
        model.addAttribute("confirmDelete", true);
    }
}
