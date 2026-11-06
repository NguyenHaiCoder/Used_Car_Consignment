package fu.he.training.fall2026.controller.web.inspector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Nguyễn Vũ Trường An: màn Đăng tin / quản lý tin đăng (UI mock).
 * <ul>
 *   <li>{@code GET /inspector/listings} — danh sách</li>
 *   <li>{@code GET /inspector/listings/new} — tạo mới (trống)</li>
 *   <li>{@code GET /inspector/listings/new?vehicleId=} — từ Kiểm định xe, prefill xe + giá</li>
 * </ul>
 */
@Controller
@RequestMapping("/inspector/listings")
public class InspectorListingsWebController {

    private static final String LIST_VIEW = "inspector/news/list";
    private static final String CREATE_VIEW = "inspector/news/create";
    private static final DateTimeFormatter INSPECT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Biển số / màu / hộp số / nhiên liệu / giá mặc định theo id SEED Kiểm định xe. */
    private static final Map<Long, Extra> EXTRAS = Map.of(
            12L, extra("51H-678.90", "Xanh dương", "Số tự động", "Xăng", "SUV", 769_000_000L),
            11L, extra("30K-123.45", "Đỏ", "Số tự động", "Xăng", "SUV", 760_000_000L),
            10L, extra("30H-678.22", "Trắng", "Số tự động", "Xăng", "SUV", 680_000_000L),
            9L, extra("51D-345.12", "Cam", "Số tự động", "Dầu", "Bán tải", 845_000_000L),
            8L, extra("51K-789.01", "Xám", "Số tự động", "Xăng", "SUV", 1_080_000_000L),
            7L, extra("51K-564.09", "Bạc", "Số tự động", "Xăng", "SUV", 610_000_000L),
            6L, extra("30K-903.28", "Đen", "Số tự động", "Điện", "Xe điện", 890_000_000L),
            5L, extra("30A-882.16", "Trắng", "Số tự động", "Xăng", "Sedan", 425_000_000L));

    @GetMapping
    public String list() {
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long vehicleId, Model model) {
        List<Map<String, Object>> vehicles = new ArrayList<>();
        for (InspectorVehiclesWebController.MockRow row : InspectorVehiclesWebController.listingCandidates()) {
            vehicles.add(toListingVehicle(row));
        }

        List<Map<String, Object>> ready = vehicles.stream()
                .filter(v -> {
                    Object inspected = v.get("inspected");
                    return inspected != null && !String.valueOf(inspected).isBlank();
                })
                .toList();

        Map<String, Object> picked = null;
        if (vehicleId != null) {
            picked = vehicles.stream()
                    .filter(v -> vehicleId.equals(v.get("id")))
                    .findFirst()
                    .orElse(null);
        }

        model.addAttribute("ready", ready);
        model.addAttribute("picked", picked);
        model.addAttribute("staffs", staffs());
        return CREATE_VIEW;
    }

    private static Map<String, Object> toListingVehicle(InspectorVehiclesWebController.MockRow row) {
        Extra extra = EXTRAS.getOrDefault(row.getId(), extra("—", "—", "—", "—", typeLabel(row.getVehicleType()), 0L));
        boolean ready = InspectorVehiclesWebController.isReadyForListing(row);
        String inspectDate = row.getInspectDate();
        String inspected = "";
        if (ready) {
            if (inspectDate != null && !inspectDate.isBlank() && !"—".equals(inspectDate)) {
                inspected = inspectDate;
            } else {
                inspected = LocalDate.now().format(INSPECT_DATE);
            }
        }

        long proposed = InspectionUiData.findValuation(row.getCode())
                .map(v -> (BigDecimal) v.get("proposedPrice"))
                .map(BigDecimal::longValue)
                .orElse(extra.proposed);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.getId());
        map.put("code", row.getCode());
        map.put("car", row.getDisplayLabel());
        map.put("year", row.getYear());
        map.put("vin", row.getVin());
        map.put("plate", extra.plate);
        map.put("km", row.getOdometerKm());
        map.put("type", extra.typeLabel);
        map.put("color", extra.color);
        map.put("gearbox", extra.gearbox);
        map.put("fuel", extra.fuel);
        map.put("owner", row.getConsignorName());
        map.put("inspected", inspected);
        map.put("proposed", proposed);
        return map;
    }

    private static String typeLabel(String vehicleType) {
        String label = InspectionUiData.labelOf(vehicleType);
        return label == null || label.isBlank() ? Optional.ofNullable(vehicleType).orElse("—") : label;
    }

    private static List<Map<String, Object>> staffs() {
        return List.of(
                staff(1L, "Trường An"),
                staff(2L, "Minh Đức"),
                staff(3L, "Thu Hà"));
    }

    private static Map<String, Object> staff(long id, String name) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        return row;
    }

    private static Extra extra(
            String plate, String color, String gearbox, String fuel, String typeLabel, long proposed) {
        return new Extra(plate, color, gearbox, fuel, typeLabel, proposed);
    }

    private record Extra(
            String plate, String color, String gearbox, String fuel, String typeLabel, long proposed) {}
}
