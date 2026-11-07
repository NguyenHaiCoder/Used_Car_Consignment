package fu.he.training.fall2026.controller.web.documents;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nguyễn Phúc Khang: dữ liệu mock của Hồ sơ sang tên, chỉ nằm trong bộ nhớ nên khởi động lại app sẽ về như cũ.
 * Mỗi hợp đồng bán đã ký có tối đa một hồ sơ (TransferCase 1-1 SalesContract), do Nhân viên hồ sơ bấm tạo.
 * Luồng đi thẳng: Chờ xử lý → Đang xử lý → Hoàn tất. Không dùng TransferStatus.BLOCKED và cột stage.
 * Khi nối BE: thay lớp này bằng service đọc TransferCase. Tệp bằng chứng đang giữ trong bộ nhớ, nối thật thì gọi
 * FileStorageService.uploadDocument rồi lưu vào evidencePath.
 */
final class TransferUiData {

    static final String PENDING = "PENDING";
    static final String IN_PROGRESS = "IN_PROGRESS";
    static final String COMPLETED = "COMPLETED";

    static final Map<String, Map<String, String>> STATUSES = statuses();

    private static final int DEFAULT_DEADLINE_DAYS = 30;
    /** Hợp đồng mẫu đã có hồ sơ; các hợp đồng đã ký còn lại chờ Nhân viên hồ sơ tạo. */
    private static final List<Long> SEEDED = List.of(201L, 203L, 204L, 205L, 206L, 207L, 208L);
    private static final List<Map<String, Object>> CASES = new CopyOnWriteArrayList<>();
    private static final Map<Long, Evidence> EVIDENCE_FILES = new ConcurrentHashMap<>();

    /** Tệp bằng chứng người dùng đã tải lên. */
    record Evidence(String name, byte[] content) {
    }

    static {
        SEEDED.forEach(id -> SalesDealUiData.contract(id).ifPresent(contract -> CASES.add(newCase(contract))));
    }

    private TransferUiData() {
    }

    /** Hợp đồng đủ điều kiện và chưa có hồ sơ sang tên. */
    static List<Map<String, Object>> candidates() {
        return SalesDealUiData.signedContracts().stream()
                .filter(TransferUiData::canCreate)
                .sorted(Comparator.comparing((Map<String, Object> row) -> (Long) row.get("id")).reversed())
                .toList();
    }

    static boolean canCreate(Map<String, Object> contract) {
        return find(contract).isEmpty() && SalesDealUiData.transferReady(contract);
    }

    /** Thông tin hồ sơ sẽ tạo, dùng để xem trước. */
    static Map<String, Object> preview(Map<String, Object> contract) {
        return newCase(contract);
    }

    static void create(Map<String, Object> contract) {
        CASES.add(newCase(contract));
    }

    static void saveEvidence(long id, String name, byte[] content) {
        EVIDENCE_FILES.put(id, new Evidence(name, content));
    }

    static Optional<Evidence> evidence(long id) {
        return Optional.ofNullable(EVIDENCE_FILES.get(id));
    }

    static Optional<Map<String, Object>> caseOf(Map<String, Object> contract) {
        return find(contract);
    }

    static List<Map<String, Object>> cases() {
        CASES.forEach(TransferUiData::refresh);
        return CASES.stream()
                .sorted(Comparator.comparing((Map<String, Object> row) -> (Long) row.get("id")).reversed())
                .toList();
    }

    static Optional<Map<String, Object>> item(long id) {
        return cases().stream().filter(row -> Long.valueOf(id).equals(row.get("id"))).findFirst();
    }

    /** Dữ liệu timeline: bước trước trạng thái hiện tại là xong; hoàn tất thì xong hết. */
    static List<Map<String, String>> steps(Map<String, Object> row) {
        List<String> keys = new ArrayList<>(STATUSES.keySet());
        int current = keys.indexOf(row.get("status"));
        boolean completed = COMPLETED.equals(row.get("status"));
        List<Map<String, String>> steps = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String state = completed || i < current ? "done" : i == current ? "current" : "";
            steps.add(Map.of("label", STATUSES.get(keys.get(i)).get("label"), "state", state, "number", String.valueOf(i + 1)));
        }
        return steps;
    }

    /** Số ngày còn lại tới hạn xử lý, âm là đã trễ. */
    static void refresh(Map<String, Object> row) {
        row.put("daysLeft", ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse((String) row.get("deadline"))));
    }

    private static Optional<Map<String, Object>> find(Map<String, Object> contract) {
        return CASES.stream().filter(row -> row.get("id").equals(contract.get("id"))).findFirst();
    }

    private static Map<String, Object> newCase(Map<String, Object> contract) {
        long id = (Long) contract.get("id");
        boolean transferred = (Boolean) contract.get("transferred");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", String.format("ST-%06d", id));
        row.put("contractCode", contract.get("code"));
        row.put("salesStaff", contract.get("staff"));
        for (String key : List.of("signedOn", "salePrice", "buyer", "phone", "buyerIdNumber", "buyerAddress",
                "car", "file", "plate", "km", "color", "fuel", "owner", "vin")) {
            row.put(key, contract.get(key));
        }
        row.put("status", transferred ? COMPLETED : PENDING);
        row.put("staff", transferred ? "Phúc Khang" : "");
        row.put("deadline", LocalDate.parse((String) contract.get("signedOn")).plusDays(DEFAULT_DEADLINE_DAYS).toString());
        row.put("notes", "");
        row.put("evidence", transferred ? "dang-ky-xe-moi-" + contract.get("plate") + ".pdf" : "");
        row.put("completedOn", transferred ? contract.get("signedOn") : "");
        applySample(row, id);
        return row;
    }

    /** Hai hồ sơ mẫu đang làm dở, có một hồ sơ trễ hạn. */
    private static void applySample(Map<String, Object> row, long id) {
        LocalDate today = LocalDate.now();
        if (id == 207) {
            sample(row, "Ngọc Mai", today.plusDays(5));
        } else if (id == 205) {
            sample(row, "Đức Long", today.minusDays(2));
        }
    }

    private static void sample(Map<String, Object> row, String staff, LocalDate deadline) {
        row.put("status", IN_PROGRESS);
        row.put("staff", staff);
        row.put("deadline", deadline.toString());
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(PENDING, status("Chờ xử lý", "slate", "hourglass", "draft"));
        map.put(IN_PROGRESS, status("Đang xử lý", "blue", "refresh", "signed"));
        map.put(COMPLETED, status("Hoàn tất", "green", "check", "listed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
