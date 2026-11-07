package fu.he.training.fall2026.controller.web.handover;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vũ Mạnh Cường: mock của Bàn giao xe, chỉ nằm trong bộ nhớ nên khởi động lại app sẽ về như cũ.
 * Khi nối BE phải XÓA HẲN lớp này, thay bằng service đọc Handover và HandoverItem (xem agent/VuManhCuongQuyTac.txt).
 * Mỗi hợp đồng đã ký có một dòng bàn giao. Trạng thái suy từ hợp đồng: phải thu đủ và sang tên xong mới được bàn giao.
 */
final class HandoverUiData {

    static final String PENDING = "PENDING";
    static final String READY = "READY";
    static final String COMPLETED = "COMPLETED";
    static final String CLOSED = "CLOSED";

    static final Map<String, Map<String, String>> STATUSES = statuses();
    static final List<String> ITEMS = List.of("Đăng ký xe mới", "Sổ bảo hiểm", "Sổ bảo dưỡng", "Lốp dự phòng", "Bộ dụng cụ");

    /** Thông tin đã nhập lúc bàn giao, theo id hợp đồng. */
    private static final Map<Long, Map<String, Object>> RECORDS = new ConcurrentHashMap<>();

    private HandoverUiData() {
    }

    static List<Map<String, Object>> rows() {
        return SalesDealUiData.signedContracts().stream()
                .sorted(Comparator.comparing((Map<String, Object> c) -> (Long) c.get("id")).reversed())
                .map(HandoverUiData::row)
                .toList();
    }

    static Optional<Map<String, Object>> item(long id) {
        return SalesDealUiData.signedContracts().stream()
                .filter(c -> Long.valueOf(id).equals(c.get("id")))
                .findFirst()
                .map(HandoverUiData::row);
    }

    static void save(long id, Map<String, Object> record) {
        RECORDS.put(id, record);
    }

    private static Map<String, Object> row(Map<String, Object> contract) {
        long id = (Long) contract.get("id");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", String.format("BG-%06d", id));
        row.put("contractCode", contract.get("code"));
        for (String key : List.of("car", "plate", "file", "km", "color", "vin", "buyer", "phone", "buyerIdNumber",
                "buyerAddress", "signedOn", "salePrice", "paid", "transferred", "handedOver")) {
            row.put(key, contract.get(key));
        }
        row.put("status", status(contract));
        row.putAll(RECORDS.getOrDefault(id, emptyRecord(contract)));
        return row;
    }

    private static String status(Map<String, Object> contract) {
        if ("CLOSED".equals(contract.get("status"))) {
            return CLOSED;
        }
        if (Boolean.TRUE.equals(contract.get("handedOver"))) {
            return COMPLETED;
        }
        return Boolean.TRUE.equals(contract.get("paid")) && Boolean.TRUE.equals(contract.get("transferred")) ? READY : PENDING;
    }

    /** Hồ sơ mẫu đã bàn giao thì coi như đủ phụ kiện, chưa bàn giao thì để trống. */
    private static Map<String, Object> emptyRecord(Map<String, Object> contract) {
        boolean done = Boolean.TRUE.equals(contract.get("handedOver"));
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("staff", done ? "Mạnh Cường" : "");
        record.put("odo", contract.get("km"));
        record.put("keys", 2);
        record.put("items", done ? ITEMS : List.of());
        record.put("notes", "");
        record.put("handedOn", done ? LocalDate.parse((String) contract.get("signedOn")).plusDays(5).toString() : "");
        record.put("staffSignature", "");
        record.put("receiverSignature", "");
        return record;
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(PENDING, status("Chờ điều kiện", "slate", "hourglass", "draft"));
        map.put(READY, status("Sẵn sàng", "blue", "car", "signed"));
        map.put(COMPLETED, status("Đã bàn giao", "amber", "handshake", "pending"));
        map.put(CLOSED, status("Đã đóng sale", "green", "check", "listed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
