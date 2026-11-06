package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.inspection.VehicleTypeRequest;
import fu.he.training.fall2026.dto.inspection.VehicleTypeResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.VehicleTypeService;
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

/** Nguyễn Vũ Trường An: Quản lý loại xe — danh sách, thêm, sửa, ngưng / kích hoạt. */
@Controller
@RequestMapping("/inspector/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeCatalogWebController {

    private static final String BASE = "/inspector/vehicle-types";
    private static final String LIST_VIEW = "inspector/vehicle-types/list";
    private static final String CREATE_VIEW = "inspector/vehicle-types/create";
    private static final String EDIT_VIEW = "inspector/vehicle-types/edit";

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<VehicleTypeResponse> result = CatalogPageSupport.lastPageIfBeyond(
                vehicleTypeService.search(q, status, page, pageSize),
                last -> vehicleTypeService.search(q, status, last, pageSize));
        Map<CatalogStatus, Long> counts = vehicleTypeService.countByStatus();
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status.name());
        model.addAttribute("size", pageSize);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("activeCount", counts.get(CatalogStatus.ACTIVE));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new VehicleTypeRequest());
        return CREATE_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", vehicleTypeService.formFor(id));
        return EDIT_VIEW;
    }

    /** Creates the vehicle type when the hidden id is empty, otherwise updates that vehicle type. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") VehicleTypeRequest form,
            BindingResult errors,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    vehicleTypeService.create(form);
                    flash.addFlashAttribute("flashMessage", "Đã thêm loại xe \"" + form.getLabel().trim() + "\".");
                    return "redirect:" + BASE;
                }
                vehicleTypeService.update(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật loại xe \"" + form.getLabel().trim() + "\".");
                return "redirect:" + BASE;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        return form.getId() == null ? CREATE_VIEW : EDIT_VIEW;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            VehicleTypeResponse type = vehicleTypeService.deactivate(id);
            flash.addFlashAttribute("flashMessage", "Đã ngưng loại xe \"" + type.getLabel() + "\".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        VehicleTypeResponse type = vehicleTypeService.activate(id);
        flash.addFlashAttribute("flashMessage", "Đã kích hoạt loại xe \"" + type.getLabel() + "\".");
        return "redirect:" + BASE;
    }
}
