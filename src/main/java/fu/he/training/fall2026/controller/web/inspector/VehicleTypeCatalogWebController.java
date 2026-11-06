package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Nguyễn Vũ Trường An: CRUD Quản lý loại xe (mock in-memory, phục vụ select loại kiểm định). */
@Controller
@RequestMapping("/inspector/vehicle-types")
public class VehicleTypeCatalogWebController {

    private static final String BASE = "/inspector/vehicle-types";

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        String statusFilter = status == null ? "" : status.trim();
        List<Map<String, Object>> filtered = InspectionUiData.vehicleTypes().stream()
                .filter(t -> statusFilter.isBlank() || statusFilter.equals(t.get("status")))
                .filter(t -> keyword.isBlank()
                        || String.valueOf(t.get("code")).toLowerCase().contains(keyword)
                        || String.valueOf(t.get("label")).toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("page", CatalogPageSupport.pageOf(filtered, page, size));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", statusFilter);
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
        model.addAttribute("total", InspectionUiData.vehicleTypes().size());
        model.addAttribute(
                "activeCount",
                InspectionUiData.vehicleTypes().stream().filter(t -> "ACTIVE".equals(t.get("status"))).count());
        return "inspector/vehicle-types/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", emptyForm());
        return "inspector/vehicle-types/create";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", InspectionUiData.findVehicleType(id));
        return "inspector/vehicle-types/edit";
    }

    @PostMapping
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String code,
            @RequestParam String label,
            @RequestParam(required = false) String status,
            RedirectAttributes flash) {
        try {
            Map<String, Object> saved = InspectionUiData.saveVehicleType(id, code, label, status);
            if (id == null) {
                flash.addFlashAttribute(
                        "flashMessage", "Đã thêm loại xe \"" + saved.get("label") + "\".");
                return "redirect:" + BASE;
            }
            flash.addFlashAttribute(
                    "flashMessage", "Đã cập nhật loại xe \"" + saved.get("label") + "\".");
            return "redirect:" + BASE;
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("flashMessage", ex.getMessage());
            return id == null ? "redirect:" + BASE + "/new" : "redirect:" + BASE + "/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        Map<String, Object> item = InspectionUiData.findVehicleType(id);
        InspectionUiData.setVehicleTypeStatus(id, "INACTIVE");
        flash.addFlashAttribute("flashMessage", "Đã ngưng loại xe \"" + item.get("label") + "\".");
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        Map<String, Object> item = InspectionUiData.findVehicleType(id);
        InspectionUiData.setVehicleTypeStatus(id, "ACTIVE");
        flash.addFlashAttribute("flashMessage", "Đã kích hoạt loại xe \"" + item.get("label") + "\".");
        return "redirect:" + BASE;
    }

    private static Map<String, Object> emptyForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", null);
        form.put("code", "");
        form.put("label", "");
        form.put("status", "ACTIVE");
        return form;
    }
}
