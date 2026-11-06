package fu.he.training.fall2026.controller.web.inspector;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Nguyễn Vũ Trường An: mock danh mục kiểm định (loại xe + loại kiểm định + định giá) — UI, chưa gắn BE.
 */
public final class InspectionUiData {

    private static final AtomicLong VEHICLE_TYPE_SEQ = new AtomicLong(4);

    private static final CopyOnWriteArrayList<Map<String, Object>> VEHICLE_TYPES = new CopyOnWriteArrayList<>(List.of(
            vehicleType(1L, "SUV", "SUV / Crossover", "ACTIVE"),
            vehicleType(2L, "SEDAN", "Sedan", "ACTIVE"),
            vehicleType(3L, "PICKUP", "Bán tải", "ACTIVE"),
            vehicleType(4L, "EV", "Xe điện", "ACTIVE")));

    public static final List<Map<String, Object>> CHECKLISTS = List.of(
            checklist(1L, "Loại kiểm định SUV / Crossover", "SUV", "ACTIVE", 8),
            checklist(2L, "Loại kiểm định Sedan", "SEDAN", "ACTIVE", 7),
            checklist(3L, "Loại kiểm định bán tải", "PICKUP", "ACTIVE", 9),
            checklist(4L, "Loại kiểm định xe điện", "EV", "ACTIVE", 8),
            checklist(5L, "Loại kiểm định SUV (bản nháp)", "SUV", "INACTIVE", 5));

    /** Định giá theo mã hồ sơ (KG-xxx) — dùng lại trên hồ sơ / hợp đồng. */
    private static final ConcurrentHashMap<String, Map<String, Object>> VALUATIONS_BY_CODE = new ConcurrentHashMap<>();

    private InspectionUiData() {}

    public static void saveValuation(String code, BigDecimal proposedPrice, String rationale) {
        if (code == null || code.isBlank() || proposedPrice == null) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code.trim());
        row.put("proposedPrice", proposedPrice);
        row.put("rationale", rationale == null ? "" : rationale.trim());
        VALUATIONS_BY_CODE.put(code.trim(), row);
    }

    public static Optional<Map<String, Object>> findValuation(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALUATIONS_BY_CODE.get(code.trim()));
    }

    public static String listingPriceText(String code) {
        return findValuation(code)
                .map(v -> (BigDecimal) v.get("proposedPrice"))
                .map(InspectionUiData::formatVnd)
                .orElse("");
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount);
    }

    public static List<Map<String, Object>> vehicleTypes() {
        return List.copyOf(VEHICLE_TYPES);
    }

    public static List<Map<String, Object>> activeVehicleTypes() {
        return VEHICLE_TYPES.stream().filter(t -> "ACTIVE".equals(t.get("status"))).toList();
    }

    public static Map<String, Object> findVehicleType(Long id) {
        return VEHICLE_TYPES.stream()
                .filter(t -> id.equals(t.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại xe: " + id));
    }

    public static Map<String, Object> saveVehicleType(Long id, String code, String label, String status) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase().replace(' ', '_');
        String normalizedLabel = label == null ? "" : label.trim();
        String normalizedStatus = status == null || status.isBlank() ? "ACTIVE" : status;
        if (normalizedCode.isEmpty() || normalizedLabel.isEmpty()) {
            throw new IllegalArgumentException("Mã và tên loại xe không được trống");
        }
        boolean duplicate = VEHICLE_TYPES.stream()
                .anyMatch(t -> normalizedCode.equals(t.get("code")) && (id == null || !id.equals(t.get("id"))));
        if (duplicate) {
            throw new IllegalArgumentException("Mã loại xe đã tồn tại: " + normalizedCode);
        }
        if (id == null) {
            Map<String, Object> row = vehicleType(VEHICLE_TYPE_SEQ.incrementAndGet(), normalizedCode, normalizedLabel, normalizedStatus);
            VEHICLE_TYPES.add(row);
            return row;
        }
        Map<String, Object> existing = findVehicleType(id);
        Map<String, Object> updated = vehicleType(id, normalizedCode, normalizedLabel, normalizedStatus);
        int index = VEHICLE_TYPES.indexOf(existing);
        VEHICLE_TYPES.set(index, updated);
        return updated;
    }

    public static void setVehicleTypeStatus(Long id, String status) {
        Map<String, Object> existing = findVehicleType(id);
        Map<String, Object> updated = vehicleType(
                id,
                String.valueOf(existing.get("code")),
                String.valueOf(existing.get("label")),
                status);
        VEHICLE_TYPES.set(VEHICLE_TYPES.indexOf(existing), updated);
    }

    public static String labelOf(String vehicleTypeCode) {
        return VEHICLE_TYPES.stream()
                .filter(t -> vehicleTypeCode != null && vehicleTypeCode.equals(t.get("code")))
                .map(t -> String.valueOf(t.get("label")))
                .findFirst()
                .orElse(vehicleTypeCode == null ? "" : vehicleTypeCode);
    }

    public static List<Map<String, Object>> activeChecklists() {
        return CHECKLISTS.stream().filter(c -> "ACTIVE".equals(c.get("status"))).toList();
    }

    public static Map<String, Object> checklistByVehicleType(String vehicleType) {
        return activeChecklists().stream()
                .filter(c -> vehicleType != null && vehicleType.equals(c.get("vehicleType")))
                .findFirst()
                .orElseGet(() -> activeChecklists().isEmpty() ? CHECKLISTS.get(0) : activeChecklists().get(0));
    }

    public static List<Map<String, Object>> checklistSteps(String vehicleType) {
        return switch (vehicleType == null ? "" : vehicleType) {
            case "SEDAN" -> List.of(
                    step(1, "Giấy tờ & VIN", "Đối chiếu đăng ký, số khung, số máy"),
                    step(2, "Ngoại thất", "Thân vỏ, sơn, kính, đèn"),
                    step(3, "Nội thất", "Ghế, taplo, điều hòa"),
                    step(4, "Động cơ / hộp số", "Rò rỉ, tiếng kêu, nhiệt độ"),
                    step(5, "Gầm & lốp", "Thanh đòn, lốp, phanh"),
                    step(6, "Điện & tiện nghi", "Màn hình, cảm biến, khóa"),
                    step(7, "Lái thử", "Tăng tốc, phanh, đánh lái"));
            case "PICKUP" -> List.of(
                    step(1, "Giấy tờ & VIN", "Đối chiếu đăng ký, số khung, số máy"),
                    step(2, "Thùng hàng / lót thùng", "Biến dạng, gỉ sét, khóa thùng"),
                    step(3, "Ngoại thất cabin", "Cửa, gương, đèn"),
                    step(4, "Khung gầm tải", "Nhíp, cầu, khớp nối"),
                    step(5, "Động cơ diesel/xăng", "Turbo, dầu, két nước"),
                    step(6, "Lốp & phanh tải", "Độ mòn, áp suất, má phanh"),
                    step(7, "Hệ thống kéo", "Móc kéo, điện trailer"),
                    step(8, "Nội thất cabin", "Ghế, điều hòa"),
                    step(9, "Lái thử có tải nhẹ", "Ly hợp, số, phanh"));
            case "EV" -> List.of(
                    step(1, "Giấy tờ & VIN", "Đối chiếu đăng ký, số khung"),
                    step(2, "Pin & SOH", "Dung lượng còn lại, báo lỗi BMS"),
                    step(3, "Cổng sạc", "AC/DC, khóa cổng, cáp"),
                    step(4, "Ngoại thất", "Thân vỏ, kính, đèn LED"),
                    step(5, "Nội thất & màn hình", "HMI, điều hòa nhiệt"),
                    step(6, "Động cơ điện / giảm tốc", "Tiếng ồn, rung"),
                    step(7, "Phanh tái sinh", "Chế độ regen, ABS"),
                    step(8, "Lái thử EV", "Tăng tốc, coasting, sạc nhanh mô phỏng"));
            default -> List.of(
                    step(1, "Giấy tờ & VIN", "Đối chiếu đăng ký, số khung, số máy"),
                    step(2, "Ngoại thất SUV", "Cản, cốp, ray nóc"),
                    step(3, "Khoang hành lý", "Sàn, lốp dự phòng"),
                    step(4, "Nội thất", "Hàng ghế 2/3, cốp điện"),
                    step(5, "Động cơ / AWD", "Hệ dẫn động, vi sai"),
                    step(6, "Gầm & treo", "Thanh cân bằng, cao độ"),
                    step(7, "An toàn ADAS", "Camera, radar"),
                    step(8, "Lái thử địa hình nhẹ", "Off-road mode, độ bám"));
        };
    }

    private static Map<String, Object> vehicleType(Long id, String code, String label, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", code);
        row.put("label", label);
        row.put("status", status);
        return row;
    }

    private static Map<String, Object> checklist(
            Long id, String name, String vehicleType, String status, int stepCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("vehicleType", vehicleType);
        row.put("vehicleTypeLabel", labelOf(vehicleType));
        row.put("status", status);
        row.put("stepCount", stepCount);
        return row;
    }

    private static Map<String, Object> step(int order, String title, String hint) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("order", order);
        row.put("title", title);
        row.put("hint", hint);
        return row;
    }
}
