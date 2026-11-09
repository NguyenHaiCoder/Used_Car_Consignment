package fu.he.training.fall2026.constant.finance;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nhãn trạng thái thu / hoàn cọc (Finance). */
public final class PaymentCatalog {

    public static final Map<String, Map<String, String>> STATUSES = statuses();

    private PaymentCatalog() {
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PARTIAL", status("Còn phải thu", "amber", "coins", "pending"));
        map.put("PAID", status("Đã thu đủ", "green", "check", "listed"));
        map.put("REFUND_PENDING", status("Cần hoàn cọc", "red", "undo", "sold"));
        map.put("REFUNDED", status("Đã hoàn cọc", "slate", "history", "closed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
