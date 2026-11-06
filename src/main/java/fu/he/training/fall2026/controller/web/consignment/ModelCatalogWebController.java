package fu.he.training.fall2026.controller.web.consignment;

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

/** UI-only CRUD dòng xe (mock data, chưa gắn service BE). */
@Controller
@RequestMapping("/consignment/models")
public class ModelCatalogWebController {

    private static final String BASE = "/consignment/models";

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        String brandFilter = brand == null ? "" : brand.trim();
        String statusFilter = status == null ? "" : status.trim();
        List<Map<String, Object>> filtered = CatalogUiData.MODELS.stream()
                .filter(m -> brandFilter.isBlank() || brandFilter.equals(m.get("brandName")))
                .filter(m -> statusFilter.isBlank() || statusFilter.equals(m.get("status")))
                .filter(m -> keyword.isBlank()
                        || String.valueOf(m.get("name")).toLowerCase().contains(keyword)
                        || String.valueOf(m.get("brandName")).toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("page", CatalogPageSupport.pageOf(filtered, page, size));
        model.addAttribute("brands", CatalogUiData.brandNames());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("brand", brandFilter);
        model.addAttribute("status", statusFilter);
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
        model.addAttribute("total", CatalogUiData.MODELS.size());
        model.addAttribute("activeCount",
                CatalogUiData.MODELS.stream().filter(m -> "ACTIVE".equals(m.get("status"))).count());
        return "consignment/models/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", emptyForm());
        model.addAttribute("brands", CatalogUiData.brandNames());
        model.addAttribute("mode", "create");
        return "consignment/models/create";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("item", find(id));
        return "consignment/models/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", find(id));
        model.addAttribute("brands", CatalogUiData.brandNames());
        model.addAttribute("mode", "edit");
        return "consignment/models/edit";
    }

    @PostMapping
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name) {
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

    private static Map<String, Object> find(Long id) {
        return CatalogUiData.MODELS.stream()
                .filter(m -> id.equals(m.get("id")))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", id);
                    row.put("brandName", "Hyundai");
                    row.put("name", "Tucson");
                    row.put("status", "ACTIVE");
                    row.put("note", "");
                    return row;
                });
    }

    private static Map<String, Object> emptyForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", null);
        form.put("brandName", "");
        form.put("name", "");
        form.put("status", "ACTIVE");
        form.put("note", "");
        return form;
    }
}
