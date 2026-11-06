package fu.he.training.fall2026.controller.web.consignment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mock danh mục hãng/dòng xe (UI, chưa gắn BE). Loại xe / loại kiểm định nằm ở InspectionUiData. */
public final class CatalogUiData {

    private CatalogUiData() {}

    public static final List<Map<String, Object>> BRANDS = List.of(
            brand(1L, "Hyundai", "ACTIVE", 5),
            brand(2L, "Toyota", "ACTIVE", 8),
            brand(3L, "Honda", "ACTIVE", 4),
            brand(4L, "Kia", "ACTIVE", 3),
            brand(5L, "Mazda", "INACTIVE", 2));

    public static final List<Map<String, Object>> MODELS = List.of(
            model(1L, "Hyundai", "Tucson", "ACTIVE"),
            model(2L, "Hyundai", "Santa Fe", "ACTIVE"),
            model(3L, "Hyundai", "Accent", "ACTIVE"),
            model(4L, "Toyota", "Camry", "ACTIVE"),
            model(5L, "Toyota", "Vios", "ACTIVE"),
            model(6L, "Honda", "Civic", "INACTIVE"),
            model(7L, "Kia", "Seltos", "ACTIVE"),
            model(8L, "Mazda", "CX-5", "ACTIVE"));

    public static List<Map<String, Object>> activeModels() {
        return MODELS.stream().filter(m -> "ACTIVE".equals(m.get("status"))).toList();
    }

    public static List<String> brandNames() {
        return BRANDS.stream().map(b -> String.valueOf(b.get("name"))).toList();
    }

    private static Map<String, Object> brand(Long id, String name, String status, int modelCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("status", status);
        row.put("modelCount", modelCount);
        row.put("note", "");
        return row;
    }

    private static Map<String, Object> model(Long id, String brandName, String name, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("brandName", brandName);
        row.put("name", name);
        row.put("status", status);
        row.put("note", "");
        return row;
    }
}
