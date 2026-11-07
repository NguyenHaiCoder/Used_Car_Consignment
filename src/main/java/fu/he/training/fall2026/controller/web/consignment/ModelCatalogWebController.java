package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.catalog.VehicleModelRequest;
import fu.he.training.fall2026.dto.catalog.VehicleModelResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.BrandService;
import fu.he.training.fall2026.service.VehicleModelService;
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

/** Quản lý dòng xe — list / CRUD / kích hoạt / ngưng (BE). */
@Controller
@RequestMapping("/consignment/models")
@RequiredArgsConstructor
public class ModelCatalogWebController {

    private static final String BASE = "/consignment/models";
    private static final String LIST_VIEW = "consignment/models/list";
    private static final String CREATE_VIEW = "consignment/models/create";
    private static final String EDIT_VIEW = "consignment/models/edit";
    private static final String VIEW = "consignment/models/view";

    private final VehicleModelService vehicleModelService;
    private final BrandService brandService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<VehicleModelResponse> result = CatalogPageSupport.lastPageIfBeyond(
                vehicleModelService.search(q, status, brandId, page, pageSize),
                last -> vehicleModelService.search(q, status, brandId, last, pageSize));
        Map<CatalogStatus, Long> counts = vehicleModelService.countByStatus();
        model.addAttribute("page", result);
        model.addAttribute("brands", brandService.listActive());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status.name());
        model.addAttribute("brandId", brandId);
        model.addAttribute("size", pageSize);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("activeCount", counts.getOrDefault(CatalogStatus.ACTIVE, 0L));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new VehicleModelRequest());
        model.addAttribute("brands", brandService.listSelectable(null));
        return CREATE_VIEW;
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("item", vehicleModelService.get(id));
        return VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        VehicleModelRequest form = vehicleModelService.formFor(id);
        model.addAttribute("form", form);
        model.addAttribute("brands", brandService.listSelectable(form.getBrandId()));
        return EDIT_VIEW;
    }

    /** Tạo khi id rỗng; cập nhật khi form có id (hidden). */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") VehicleModelRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = vehicleModelService.create(form);
                    flash.addFlashAttribute("flashMessage", "Đã thêm dòng xe \"" + form.getName().trim() + "\".");
                    return "redirect:" + BASE + "/" + id;
                }
                vehicleModelService.update(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật dòng xe \"" + form.getName().trim() + "\".");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("brands", brandService.listSelectable(form.getBrandId()));
        return form.getId() == null ? CREATE_VIEW : EDIT_VIEW;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            VehicleModelResponse item = vehicleModelService.deactivate(id);
            flash.addFlashAttribute("flashMessage", "Đã ngưng dòng xe \"" + item.getName() + "\".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        VehicleModelResponse item = vehicleModelService.activate(id);
        flash.addFlashAttribute("flashMessage", "Đã kích hoạt dòng xe \"" + item.getName() + "\".");
        return "redirect:" + BASE;
    }
}
