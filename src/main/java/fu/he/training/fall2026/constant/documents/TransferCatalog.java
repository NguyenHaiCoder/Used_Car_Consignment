package fu.he.training.fall2026.constant.documents;

import fu.he.training.fall2026.entity.enums.TransferStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nhãn trạng thái hồ sơ sang tên. */
public final class TransferCatalog {

    public static final Map<String, Map<String, String>> STATUSES = statuses();

    private TransferCatalog() {
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(TransferStatus.PENDING.name(), status("Chờ xử lý", "slate", "hourglass", "draft"));
        map.put(TransferStatus.IN_PROGRESS.name(), status("Đang xử lý", "blue", "refresh", "signed"));
        map.put(TransferStatus.COMPLETED.name(), status("Hoàn tất", "green", "check", "listed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
