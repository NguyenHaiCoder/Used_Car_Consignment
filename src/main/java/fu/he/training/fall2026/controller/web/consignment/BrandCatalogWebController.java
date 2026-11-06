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

/** UI-only CRUD hãng xe (mock data, chưa gắn service BE). */
@Controller
@RequestMapping("/consignment/brands")
public class BrandCatalogWebController {

    private static final String BASE = "/consignment/brands";

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        String statusFilter = status == null ? "" : status.trim();
        List<Map<String, Object>> filtered = CatalogUiData.BRANDS.stream()
                .filter(b -> statusFilter.isBlank() || statusFilter.equals(b.get("status")))
                .filter(b -> keyword.isBlank()
                        || String.valueOf(b.get("name")).toLowerCase().contains(keyword))
                .toList();
        model.addAttribute("page", CatalogPageSupport.pageOf(filtered, page, size));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", statusFilter);
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
        model.addAttribute("total", CatalogUiData.BRANDS.size());
        model.addAttribute("activeCount",
                CatalogUiData.BRANDS.stream().filter(b -> "ACTIVE".equals(b.get("status"))).count());
        return "consignment/brands/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", emptyForm());
        model.addAttribute("mode", "create");
        return "consignment/brands/create";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("item", find(id));
        return "consignment/brands/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", find(id));
        model.addAttribute("mode", "edit");
        return "consignment/brands/edit";
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
        return CatalogUiData.BRANDS.stream()
                .filter(b -> id.equals(b.get("id")))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", id);
                    row.put("name", "Hyundai");
                    row.put("status", "ACTIVE");
                    row.put("modelCount", 0);
                    row.put("note", "");
                    return row;
                });
    }

    private static Map<String, Object> emptyForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", null);
        form.put("name", "");
        form.put("status", "ACTIVE");
        form.put("note", "");
        form.put("modelCount", 0);
        return form;
    }
}
