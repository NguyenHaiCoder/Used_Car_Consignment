package fu.he.training.fall2026.controller.web.inspector;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Nguyễn Vũ Trường An: Kiểm định xe (mock) — Tiếp nhận → Đang kiểm định → màn checklist theo loại xe.
 */
@Controller
@RequestMapping("/inspector/vehicles")
public class InspectorVehiclesWebController {

    private static final String LIST_VIEW = "inspector/vehicles/list";
    private static final String INSPECT_VIEW = "inspector/vehicles/inspect";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INSPECTING = "INSPECTION_RECEIVED";
    private static final String STATUS_INSPECTED = "INSPECTED";

    private static final Map<Long, String> STATUS_BY_ID = new ConcurrentHashMap<>();

    private static final List<MockRow> SEED = List.of(
            row(12, "KG-112", "Hyundai Tucson 2.0", 2022, "KMHJ3812NNU112001", "Nguyễn Quang Huy", 32000, "06/11/2026", "—", "SUV"),
            row(11, "KG-111", "Mazda CX-5 2.0 Luxury", 2021, "JM7KF2WLA00111002", "Lê Thị Mai", 48500, "05/11/2026", "—", "SUV"),
            row(10, "KG-110", "Kia Seltos 1.6 Premium", 2023, "KNAE351AAPK110003", "Đỗ Minh Đức", 18200, "04/11/2026", "06/11/2026", "SUV"),
            row(9, "KG-109", "Ford Ranger Wildtrak", 2020, "MNAUM2F50LW109004", "Phạm Quốc Huy", 76000, "03/11/2026", "—", "PICKUP"),
            row(8, "KG-108", "Honda CR-V 1.5L", 2022, "MRHRE1870NP108005", "Trần Văn Bình", 27400, "31/10/2026", "02/11/2026", "SUV"),
            row(7, "KG-107", "Mitsubishi Xpander 1.5 AT", 2023, "MMABNKI40PD107006", "Ngô Thị Lan", 21300, "30/10/2026", "—", "SUV"),
            row(6, "KG-106", "VinFast VF 8 Plus", 2023, "RLXVAFF45P3107007", "Hoàng Anh Tuấn", 15600, "29/10/2026", "01/11/2026", "EV"),
            row(5, "KG-105", "Toyota Vios 1.5G", 2021, "MR2B29F30M1105008", "Vũ Thị Hồng", 54200, "27/10/2026", "—", "SEDAN"));

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        String keyword = q == null ? "" : q.trim();
        List<MockRow> shown = new ArrayList<>();
        for (MockRow seed : SEED) {
            MockRow row = withLiveStatus(seed);
            if (keyword.isEmpty() || matches(row, keyword)) {
                shown.add(row);
            }
        }

        int pageSize = CatalogPageSupport.normalizeSize(size);
        model.addAttribute("page", CatalogPageSupport.pageOf(shown, page, pageSize));
        model.addAttribute("q", keyword);
        model.addAttribute("size", pageSize);
        model.addAttribute("status", "");
        model.addAttribute("approved", shown);
        model.addAttribute("statusNames", statusNames());
        model.addAttribute("statusTones", statusTones());
        return LIST_VIEW;
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable long id, RedirectAttributes flash) {
        STATUS_BY_ID.put(id, STATUS_INSPECTING);
        flash.addFlashAttribute("flashMessage", "Đã tiếp nhận — trạng thái: Đang kiểm định.");
        return "redirect:/inspector/vehicles";
    }

    /** Màn tiến hành kiểm định — chọn loại kiểm định + ảnh chứng minh (UI mock). */
    @GetMapping("/{id}/inspect")
    public String inspect(
            @PathVariable long id,
            @RequestParam(required = false) Long checklistId,
            Model model) {
        MockRow vehicle = SEED.stream()
                .filter(r -> r.getId() == id)
                .map(InspectorVehiclesWebController::withLiveStatus)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe: " + id));
        List<Map<String, Object>> checklists = InspectionUiData.activeChecklists();
        Map<String, Object> checklist = checklists.stream()
                .filter(c -> checklistId != null && checklistId.equals(c.get("id")))
                .findFirst()
                .orElseGet(() -> InspectionUiData.checklistByVehicleType(vehicle.getVehicleType()));
        String vehicleTypeCode = String.valueOf(checklist.get("vehicleType"));
        Map<String, Object> valuation = InspectionUiData.findValuation(vehicle.getCode()).orElse(null);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("checklists", checklists);
        model.addAttribute("checklist", checklist);
        model.addAttribute("selectedChecklistId", checklist.get("id"));
        model.addAttribute("steps", InspectionUiData.checklistSteps(vehicleTypeCode));
        model.addAttribute("valuation", valuation);
        return INSPECT_VIEW;
    }

    @PostMapping("/{id}/inspect")
    public String submitInspect(
            @PathVariable long id,
            @RequestParam(required = false) Long checklistId,
            @RequestParam BigDecimal proposedPrice,
            @RequestParam(required = false) String rationale,
            RedirectAttributes flash) {
        MockRow vehicle = SEED.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe: " + id));
        InspectionUiData.saveValuation(vehicle.getCode(), proposedPrice, rationale);
        STATUS_BY_ID.put(id, STATUS_INSPECTED);
        String priceText = InspectionUiData.formatVnd(proposedPrice);
        flash.addFlashAttribute(
                "flashMessage",
                "Đã lưu kiểm định & định giá " + priceText + " đ — cập nhật hồ sơ và hợp đồng (giá chào bán)"
                        + (checklistId == null ? "" : " · loại #" + checklistId)
                        + ".");
        return "redirect:/inspector/vehicles";
    }

    /** Dùng cho màn Đăng tin: danh sách xe theo trạng thái kiểm định hiện tại. */
    static List<MockRow> listingCandidates() {
        List<MockRow> rows = new ArrayList<>(SEED.size());
        for (MockRow seed : SEED) {
            rows.add(withLiveStatus(seed));
        }
        return rows;
    }

    static Optional<MockRow> findListingCandidate(long id) {
        return SEED.stream()
                .filter(r -> r.getId() == id)
                .map(InspectorVehiclesWebController::withLiveStatus)
                .findFirst();
    }

    static boolean isReadyForListing(MockRow row) {
        if (row == null) {
            return false;
        }
        if (STATUS_INSPECTED.equals(row.getStatus())) {
            return true;
        }
        String inspected = row.getInspectDate();
        return inspected != null && !inspected.isBlank() && !"—".equals(inspected);
    }

    private static MockRow withLiveStatus(MockRow seed) {
        String status = STATUS_BY_ID.getOrDefault(seed.getId(), seed.getStatus());
        if (status.equals(seed.getStatus())) {
            return seed;
        }
        return new MockRow(
                seed.getId(),
                seed.getCode(),
                seed.getDisplayLabel(),
                seed.getYear(),
                seed.getVin(),
                seed.getConsignorName(),
                seed.getOdometerKm(),
                seed.getIntakeDate(),
                seed.getInspectDate(),
                seed.getVehicleType(),
                status);
    }

    private static boolean matches(MockRow row, String keyword) {
        String haystack = (row.getCode() + " " + row.getDisplayLabel() + " " + row.getVin() + " " + row.getConsignorName())
                .toLowerCase(Locale.ROOT);
        return haystack.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String> statusNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put(STATUS_ACTIVE, "Đã duyệt");
        names.put(STATUS_INSPECTING, "Đang kiểm định");
        names.put(STATUS_INSPECTED, "Đã kiểm định");
        return names;
    }

    private static Map<String, String> statusTones() {
        Map<String, String> tones = new LinkedHashMap<>();
        tones.put(STATUS_ACTIVE, "approved");
        tones.put(STATUS_INSPECTING, "pending");
        tones.put(STATUS_INSPECTED, "approved");
        return tones;
    }

    private static MockRow row(
            long id,
            String code,
            String displayLabel,
            int year,
            String vin,
            String consignorName,
            int odometerKm,
            String intakeDate,
            String inspectDate,
            String vehicleType) {
        return new MockRow(
                id,
                code,
                displayLabel,
                year,
                vin,
                consignorName,
                odometerKm,
                intakeDate,
                inspectDate,
                vehicleType,
                STATUS_ACTIVE);
    }

    @Value
    public static class MockRow {
        long id;
        String code;
        String displayLabel;
        int year;
        String vin;
        String consignorName;
        int odometerKm;
        String intakeDate;
        String inspectDate;
        String vehicleType;
        String status;
    }
}
