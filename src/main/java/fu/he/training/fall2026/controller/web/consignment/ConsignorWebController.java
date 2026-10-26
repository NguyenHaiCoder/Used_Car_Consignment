package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignor.ConsignorForm;
import fu.he.training.fall2026.dto.consignor.ConsignorResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ConsignorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

/** 02.01 Consignor Management — private vehicle owners, created and maintained by shop staff. */
@Controller
@RequestMapping("/consignment/consignors")
@RequiredArgsConstructor
public class ConsignorWebController {

    private static final int PAGE_SIZE = 10;
    private static final String BASE = "/consignment/consignors";
    private static final String FORM_VIEW = "consignment/consignors/app-form";

    private final ConsignorService consignorService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ConsignorResponse> result = consignorService.search(q, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        return "consignment/consignors/app-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ConsignorForm());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", BASE);
        model.addAttribute("cancelUrl", BASE);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ConsignorForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long id = consignorService.create(form);
                flash.addFlashAttribute("flashMessage", "Đã tạo hồ sơ chủ xe.");
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("duplicate", ex.getMessage());
            }
        }
        model.addAttribute("editing", false);
        model.addAttribute("formAction", BASE);
        model.addAttribute("cancelUrl", BASE);
        return FORM_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("consignor", consignorService.get(id));
        return "consignment/consignors/app-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", consignorService.formFor(id));
        model.addAttribute("editing", true);
        model.addAttribute("formAction", BASE + "/" + id);
        model.addAttribute("cancelUrl", BASE + "/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ConsignorForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                consignorService.update(id, form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật hồ sơ chủ xe.");
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("duplicate", ex.getMessage());
            }
        }
        model.addAttribute("editing", true);
        model.addAttribute("formAction", BASE + "/" + id);
        model.addAttribute("cancelUrl", BASE + "/" + id);
        return FORM_VIEW;
    }
}
