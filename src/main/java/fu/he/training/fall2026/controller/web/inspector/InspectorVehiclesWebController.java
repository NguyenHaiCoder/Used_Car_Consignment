package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.inspection.InspectionQueueResponse;
import fu.he.training.fall2026.dto.inspection.InspectionResultRequest;
import fu.he.training.fall2026.dto.inspection.InspectionStepResponse;
import fu.he.training.fall2026.dto.inspection.InspectionStepResultRequest;
import fu.he.training.fall2026.dto.inspection.InspectionTypeResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.InspectionTypeService;
import fu.he.training.fall2026.service.InspectorVehicleService;
import fu.he.training.fall2026.service.ValuationService;
import jakarta.validation.Valid;
import java.util.List;
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

/** Nguyễn Vũ Trường An: Kiểm định xe — Đã duyệt → Tiếp nhận → Đang kiểm định → Lưu kiểm định + định giá → Đã kiểm định. */
@Controller
@RequestMapping("/inspector/vehicles")
@RequiredArgsConstructor
public class InspectorVehiclesWebController {

    private static final String BASE = "/inspector/vehicles";
    private static final String LIST_VIEW = "inspector/vehicles/list";
    private static final String INSPECT_VIEW = "inspector/vehicles/inspect";

    private static final Map<String, String> STATUS_NAMES = Map.of(
            "ACTIVE", "Đã duyệt",
            "INSPECTION_RECEIVED", "Đang kiểm định",
            "INSPECTED", "Đã kiểm định");
    private static final Map<String, String> STATUS_TONES = Map.of(
            "ACTIVE", "approved",
            "INSPECTION_RECEIVED", "pending",
            "INSPECTED", "approved");

    private final InspectorVehicleService inspectorVehicleService;
    private final InspectionTypeService inspectionTypeService;
    private final ValuationService valuationService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<InspectionQueueResponse> result = CatalogPageSupport.lastPageIfBeyond(
                inspectorVehicleService.searchQueue(q, page, pageSize),
                last -> inspectorVehicleService.searchQueue(q, last, pageSize));
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", "");
        model.addAttribute("size", pageSize);
        model.addAttribute("statusNames", STATUS_NAMES);
        model.addAttribute("statusTones", STATUS_TONES);
        return LIST_VIEW;
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, RedirectAttributes flash) {
        try {
            inspectorVehicleService.receive(id);
            flash.addFlashAttribute("flashMessage", "Đã tiếp nhận — trạng thái: Đang kiểm định.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE;
    }

    /** Màn tiến hành kiểm định: chọn loại kiểm định, kết quả + ảnh từng bước, giá chào bán. */
    @GetMapping("/{id}/inspect")
    public String inspect(
            @PathVariable Long id,
            @RequestParam(required = false) Long checklistId,
            Model model) {
        List<InspectionTypeResponse> checklists = inspectionTypeService.listActive();
        Long selectedId = checklists.stream()
                .map(InspectionTypeResponse::getId)
                .filter(typeId -> typeId.equals(checklistId))
                .findFirst()
                .orElse(checklists.isEmpty() ? null : checklists.get(0).getId());
        model.addAttribute("vehicle", inspectorVehicleService.getForInspection(id));
        InspectionResultRequest form = inspectorVehicleService.formFor(id, selectedId);
        model.addAttribute("form", form);
        addInspectOptions(model, form, checklists, selectedId);
        return INSPECT_VIEW;
    }

    @PostMapping("/{id}/inspect")
    public String submitInspect(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InspectionResultRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                String code = inspectorVehicleService.submitInspection(id, form);
                flash.addFlashAttribute("flashMessage",
                        "Đã lưu kiểm định hồ sơ " + code + ", giá chào bán " + valuationService.listingPriceText(code)
                                + " ₫. Hồ sơ đã sẵn sàng đăng tin.");
                return "redirect:" + BASE;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("vehicle", inspectorVehicleService.getForInspection(id));
        addInspectOptions(model, form, inspectionTypeService.listActive(), form.getChecklistId());
        return INSPECT_VIEW;
    }

    /** The form keeps exactly one result row per step so the template can bind steps[i] safely. */
    private void addInspectOptions(
            Model model, InspectionResultRequest form, List<InspectionTypeResponse> checklists, Long selectedId) {
        List<InspectionStepResponse> steps = selectedId == null ? List.of() : inspectionTypeService.steps(selectedId);
        while (form.getSteps().size() < steps.size()) {
            form.getSteps().add(new InspectionStepResultRequest());
        }
        model.addAttribute("checklists", checklists);
        model.addAttribute("selectedChecklistId", selectedId);
        model.addAttribute("steps", steps);
    }
}
