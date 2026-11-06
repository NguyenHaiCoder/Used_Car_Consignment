package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Nguyễn Vũ Trường An: UI-only CRUD loại kiểm định + bước multipart (mock).
 */
@Controller
@RequestMapping("/inspector/checklists")
public class ChecklistCatalogWebController {

    private static final String BASE = "/inspector/checklists";

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        String statusFilter = status == null ? "" : status.trim();
        List<Map<String, Object>> filtered = InspectionUiData.CHECKLISTS.stream()
                .filter(c -> statusFilter.isBlank() || statusFilter.equals(c.get("status")))
                .filter(c -> keyword.isBlank()
                        || String.valueOf(c.get("name")).toLowerCase().contains(keyword)
                        || String.valueOf(c.get("vehicleTypeLabel")).toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("page", CatalogPageSupport.pageOf(filtered, page, size));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", statusFilter);
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
        model.addAttribute("total", InspectionUiData.CHECKLISTS.size());
        model.addAttribute(
                "activeCount",
                InspectionUiData.CHECKLISTS.stream().filter(c -> "ACTIVE".equals(c.get("status"))).count());
        return "inspector/checklists/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", emptyForm());
        model.addAttribute("vehicleTypes", InspectionUiData.activeVehicleTypes());
        model.addAttribute("steps", List.of());
        return "inspector/checklists/create";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Map<String, Object> item = find(id);
        model.addAttribute("item", item);
        model.addAttribute("steps", InspectionUiData.checklistSteps(String.valueOf(item.get("vehicleType"))));
        return "inspector/checklists/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Map<String, Object> item = find(id);
        model.addAttribute("form", item);
        model.addAttribute("vehicleTypes", InspectionUiData.activeVehicleTypes());
        model.addAttribute("steps", InspectionUiData.checklistSteps(String.valueOf(item.get("vehicleType"))));
        return "inspector/checklists/edit";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam String vehicleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> stepTitles,
            @RequestParam(required = false) List<String> stepHints) {
        if (id == null) {
            return "redirect:" + BASE;
        }
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        find(id);
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        find(id);
        return "redirect:" + BASE;
    }

    private static List<Map<String, Object>> buildSteps(List<String> titles, List<String> hints) {
        List<Map<String, Object>> steps = new ArrayList<>();
        if (titles == null) {
            return steps;
        }
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i) == null ? "" : titles.get(i).trim();
            if (title.isEmpty()) {
                continue;
            }
            String hint = "";
            if (hints != null && i < hints.size() && hints.get(i) != null) {
                hint = hints.get(i).trim();
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order", steps.size() + 1);
            row.put("title", title);
            row.put("hint", hint);
            steps.add(row);
        }
        return steps;
    }

    private static Map<String, Object> find(Long id) {
        return InspectionUiData.CHECKLISTS.stream()
                .filter(c -> id.equals(c.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại kiểm định: " + id));
    }

    private static Map<String, Object> emptyForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", null);
        form.put("name", "");
        form.put("vehicleType", "");
        form.put("status", "ACTIVE");
        return form;
    }
}
