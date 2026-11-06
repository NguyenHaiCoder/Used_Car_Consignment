package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.inspection.InspectionStepRequest;
import fu.he.training.fall2026.dto.inspection.InspectionTypeRequest;
import fu.he.training.fall2026.dto.inspection.InspectionTypeResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.InspectionTypeService;
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

/** Nguyễn Vũ Trường An: Loại kiểm định — danh sách, xem, thêm, sửa (kèm các bước), ngưng / kích hoạt. */
@Controller
@RequestMapping("/inspector/checklists")
@RequiredArgsConstructor
public class ChecklistCatalogWebController {

    private static final String BASE = "/inspector/checklists";
    private static final String LIST_VIEW = "inspector/checklists/list";
    private static final String CREATE_VIEW = "inspector/checklists/create";
    private static final String EDIT_VIEW = "inspector/checklists/edit";
    private static final String DETAIL_VIEW = "inspector/checklists/view";

    private final InspectionTypeService inspectionTypeService;
    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<InspectionTypeResponse> result = CatalogPageSupport.lastPageIfBeyond(
                inspectionTypeService.search(q, status, page, pageSize),
                last -> inspectionTypeService.search(q, status, last, pageSize));
        Map<CatalogStatus, Long> counts = inspectionTypeService.countByStatus();
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
        InspectionTypeRequest form = new InspectionTypeRequest();
        form.getSteps().add(new InspectionStepRequest());
        model.addAttribute("form", form);
        model.addAttribute("vehicleTypes", vehicleTypeService.listActive());
        return CREATE_VIEW;
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("item", inspectionTypeService.get(id));
        model.addAttribute("steps", inspectionTypeService.steps(id));
        return DETAIL_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InspectionTypeRequest form = inspectionTypeService.formFor(id);
        model.addAttribute("form", form);
        model.addAttribute("vehicleTypes", vehicleTypeService.listSelectable(form.getVehicleTypeId()));
        return EDIT_VIEW;
    }

    /** Creates the inspection type when the hidden id is empty, otherwise updates it; the steps are saved with it. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") InspectionTypeRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = inspectionTypeService.create(form);
                    flash.addFlashAttribute("flashMessage", "Đã thêm loại kiểm định \"" + form.getName().trim() + "\".");
                    return "redirect:" + BASE + "/" + id;
                }
                inspectionTypeService.update(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã cập nhật loại kiểm định \"" + form.getName().trim() + "\".");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        if (form.getSteps().isEmpty()) {
            form.getSteps().add(new InspectionStepRequest());
        }
        model.addAttribute("vehicleTypes", vehicleTypeService.listSelectable(form.getVehicleTypeId()));
        return form.getId() == null ? CREATE_VIEW : EDIT_VIEW;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        InspectionTypeResponse type = inspectionTypeService.deactivate(id);
        flash.addFlashAttribute("flashMessage", "Đã ngưng loại kiểm định \"" + type.getName() + "\".");
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        try {
            InspectionTypeResponse type = inspectionTypeService.activate(id);
            flash.addFlashAttribute("flashMessage", "Đã kích hoạt loại kiểm định \"" + type.getName() + "\".");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }
}
