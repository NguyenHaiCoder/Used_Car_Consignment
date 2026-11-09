package fu.he.training.fall2026.constant.inspector;

import java.util.Map;

/** Nhãn trạng thái hàng đợi kiểm định xe. */
public final class InspectorVehicleCatalog {

    public static final Map<String, String> STATUS_NAMES = Map.of(
            "ACTIVE", "Đã duyệt",
            "INSPECTION_RECEIVED", "Đang kiểm định",
            "INSPECTED", "Đã kiểm định");

    public static final Map<String, String> STATUS_TONES = Map.of(
            "ACTIVE", "approved",
            "INSPECTION_RECEIVED", "pending",
            "INSPECTED", "approved");

    private InspectorVehicleCatalog() {
    }
}
