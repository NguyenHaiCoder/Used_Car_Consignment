package fu.he.training.fall2026.constant.handover;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nhãn trạng thái bàn giao xe. */
public final class HandoverCatalog {

    public static final Map<String, Map<String, String>> STATUSES = statuses();

    private HandoverCatalog() {
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PENDING", status("Chờ điều kiện", "slate", "hourglass", "draft"));
        map.put("READY", status("Sẵn sàng", "blue", "car", "signed"));
        map.put("COMPLETED", status("Đã bàn giao", "amber", "handshake", "pending"));
        map.put("FINALIZED", status("Hoàn tất giao dịch", "green", "check", "listed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
