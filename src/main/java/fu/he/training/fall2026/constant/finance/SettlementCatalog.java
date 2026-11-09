package fu.he.training.fall2026.constant.finance;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nhãn trạng thái quyết toán / chi trả. */
public final class SettlementCatalog {

    public static final Map<String, Map<String, String>> REVIEW_STATUSES = reviewStatuses();
    public static final Map<String, Map<String, String>> STATUSES = statuses();

    private SettlementCatalog() {
    }

    private static Map<String, Map<String, String>> reviewStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("PENDING_REVIEW", status("Chờ duyệt", "amber", "hourglass", "pending"));
        map.put("APPROVED", status("Đã duyệt", "blue", "shield-check", "signed"));
        map.put("PAID_OUT", status("Đã chi", "green", "check", "listed"));
        return map;
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("DRAFT", status("Nháp", "slate", "pencil", "draft"));
        map.putAll(REVIEW_STATUSES);
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
