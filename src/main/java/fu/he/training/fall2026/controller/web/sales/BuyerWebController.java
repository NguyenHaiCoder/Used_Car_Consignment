package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.buyer.BuyerForm;
import fu.he.training.fall2026.dto.buyer.CreateBuyerRequest;
import fu.he.training.fall2026.dto.buyer.UpdateBuyerRequest;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.service.BuyerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sales/buyers")
public class BuyerWebController {
    private final BuyerService buyerService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String search, Model model) {
        model.addAttribute("buyers", buyerService.list(page, size, search));
        model.addAttribute("search", search);
        return "sales/buyers/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size, Model model) {
        model.addAttribute("buyer", buyerService.getDetails(id));
        model.addAttribute("history", buyerService.getInquiryHistory(id, page, size));
        return "sales/buyers/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("buyerForm", new BuyerForm());
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        var buyer = buyerService.getDetails(id);
        var input = new BuyerForm();
        input.setFullName(buyer.getFullName());
        input.setPhone(buyer.getPhone());
        input.setEmail(buyer.getEmail());
        input.setAddress(buyer.getAddress());
        input.setRevision(buyer.getRevision() == null ? null : buyer.getRevision().toString());
        model.addAttribute("buyerForm", input);
        return form(model, id);
    }

    @PostMapping
    public String create(@ModelAttribute("buyerForm") BuyerForm input, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return form(model, null);
        try {
            var buyer = buyerService.create(new CreateBuyerRequest(input.getFullName(), input.getPhone(),
                    input.getEmail(), input.getAddress(), input.isConsentStored()));
            redirect.addFlashAttribute("success", "Buyer created successfully.");
            return "redirect:/sales/buyers/" + buyer.getBuyerId();
        } catch (BuyerException ex) {
            formErrors(ex, binding, model);
            return form(model, null);
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable long id, @ModelAttribute("buyerForm") BuyerForm input,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return form(model, id);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("fullName", input.getFullName());
        fields.put("phone", input.getPhone());
        fields.put("email", input.getEmail());
        fields.put("address", input.getAddress());
        fields.put("revision", input.getRevision());
        try {
            buyerService.update(id, new UpdateBuyerRequest(fields));
            redirect.addFlashAttribute("success", "Buyer updated successfully.");
            return "redirect:/sales/buyers/" + id;
        } catch (BuyerException ex) {
            formErrors(ex, binding, model);
            return form(model, id);
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteConfirmation(@PathVariable long id, Model model) {
        model.addAttribute("buyer", buyerService.getDetails(id));
        model.addAttribute("confirmDelete", true);
        return "sales/buyers/details";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, Model model, RedirectAttributes redirect) {
        try {
            buyerService.delete(id);
            redirect.addFlashAttribute("success", "Buyer deleted successfully.");
            return "redirect:/sales/buyers";
        } catch (BuyerException ex) {
            model.addAttribute("error", ex.getMessage());
            return "sales/buyers/details";
        }
    }

    private String form(Model model, Long id) {
        model.addAttribute("buyerId", id);
        model.addAttribute("editing", id != null);
        return "sales/buyers/form";
    }

    private void formErrors(BuyerException ex, BindingResult binding, Model model) {
        model.addAttribute("error", ex.getMessage());
        ex.getErrors().forEach((field, message) -> binding.rejectValue(field, "buyer.invalid", message));
    }

    @ExceptionHandler(BuyerException.class)
    public String buyerError(BuyerException ex, Model model, HttpServletResponse response) {
        response.setStatus(switch (ex.getReason()) {
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case VALIDATION -> 400;
        });
        model.addAttribute("error", ex.getMessage());
        return "sales/buyers/details";
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public String invalidRequest(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute("error", "Invalid request. Please check the buyer ID, search and pagination parameters.");
        return "sales/buyers/details";
    }
}
