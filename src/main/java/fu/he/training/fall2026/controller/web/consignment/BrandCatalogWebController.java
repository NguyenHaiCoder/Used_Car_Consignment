package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.catalog.BrandRequest;
import fu.he.training.fall2026.dto.catalog.BrandResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.BrandService;
import jakarta.validation.Valid;
import java.util.Map;
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

/** Quản lý hãng xe — list / CRUD / kích hoạt / ngưng (BE). */
@Controller
@RequestMapping("/consignment/brands")
@RequiredArgsConstructor
public class BrandCatalogWebController {

    private static final String BASE = "/consignment/brands";
    private static final String LIST_VIEW = "consignment/brands/list";
    private static final String CREATE_VIEW = "consignment/brands/create";
    private static final String EDIT_VIEW = "consignment/brands/edit";
    private static final String VIEW = "consignment/brands/view";

    private final BrandService brandService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<BrandResponse> result = CatalogPageSupport.lastPageIfBeyond(
                brandService.search(q, status, page, pageSize),
                last -> brandService.search(q, status, last, pageSize));
        Map<CatalogStatus, Long> counts = brandService.countByStatus();
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status.name());
        model.addAttribute("size", pageSize);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("activeCount", counts.getOrDefault(CatalogStatus.ACTIVE, 0L));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BrandRequest());
        return CREATE_VIEW;
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("item", brandService.get(id));
        return VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", brandService.formFor(id));
        return EDIT_VIEW;
    }

    /** Tạo khi id rỗng; cập nhật khi form có id (hidden). */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") BrandRequest form,
            BindingResult errors,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = brandService.create(form);
                    flash.addFlashAttribute("flashMessage", "Đã thêm hãng xe \"" + form.getName().trim() + "\".");
                    return "redirect:" + BASE + "/" + id;
                }
                brandService.update(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật hãng xe \"" + form.getName().trim() + "\".");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        return form.getId() == null ? CREATE_VIEW : EDIT_VIEW;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            BrandResponse brand = brandService.deactivate(id);
            flash.addFlashAttribute("flashMessage", "Đã ngưng hãng xe \"" + brand.getName() + "\".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        BrandResponse brand = brandService.activate(id);
        flash.addFlashAttribute("flashMessage", "Đã kích hoạt hãng xe \"" + brand.getName() + "\".");
        return "redirect:" + BASE;
    }
}
