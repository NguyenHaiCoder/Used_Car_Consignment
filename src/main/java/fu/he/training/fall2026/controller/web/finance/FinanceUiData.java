package fu.he.training.fall2026.controller.web.finance;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Vũ Mạnh Cường: mock của Thu tiền, Hoàn cọc, Quyết toán và Chi trả, chỉ nằm trong bộ nhớ nên khởi động lại app sẽ về như cũ.
 * Khi nối BE phải XÓA HẲN lớp này, thay bằng service đọc Payment, Refund, Settlement, Payout (xem agent/VuManhCuongQuyTac.txt).
 * Cọc là khoản thu đầu tiên của hợp đồng. Quyết toán tự mở sau khi đóng sale.
 * Tiền chủ xe = giá bán − hoa hồng − phí cố định. Mock dùng 1,5% và 0 đồng; nối BE lấy từ ConsignmentAgreement.
 */
public final class FinanceUiData {

    public static final String PARTIAL = "PARTIAL";
    public static final String PAID = "PAID";
    public static final String REFUND_PENDING = "REFUND_PENDING";
    public static final String REFUNDED = "REFUNDED";

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String PAID_OUT = "PAID_OUT";

    public static final List<String> METHODS = List.of("Chuyển khoản", "Tiền mặt", "Quẹt thẻ");
    public static final Map<String, Map<String, String>> PAYMENT_STATUSES = paymentStatuses();
    public static final Map<String, Map<String, String>> SETTLEMENT_STATUSES = settlementStatuses();
    /** Manager không xem bản nháp của Finance. */
    public static final Map<String, Map<String, String>> REVIEW_STATUSES = reviewStatuses();

    private static final long COMMISSION_PER_THOUSAND = 15;
    private static final long FIXED_FEE = 0;

    private static final Map<Long, List<Map<String, Object>>> RECEIPTS = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, Object>> REFUNDS = new ConcurrentHashMap<>();
    private static final List<Map<String, Object>> SETTLEMENTS = new CopyOnWriteArrayList<>();

    static {
        seed();
    }

    private FinanceUiData() {
    }

    /** Một danh sách cho cả khoản cần thu và cọc cần hoàn. */
    static List<Map<String, Object>> paymentRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        SalesDealUiData.signedContracts().stream()
                .filter(contract -> Boolean.TRUE.equals(contract.get("paymentRequested")))
                .sorted(Comparator.comparing((Map<String, Object> c) -> (Long) c.get("id")).reversed())
                .forEach(contract -> rows.add(paymentRow(contract)));
        SalesDealUiData.refundableDeposits().forEach(reservation -> rows.add(refundRow(reservation)));
        return rows;
    }

    static Optional<Map<String, Object>> payment(long contractId) {
        return SalesDealUiData.contract(contractId)
                .filter(contract -> Boolean.TRUE.equals(contract.get("paymentRequested")))
                .map(FinanceUiData::paymentRow);
    }

    static Optional<Map<String, Object>> refund(long reservationId) {
        return SalesDealUiData.refundableDeposits().stream()
                .filter(r -> Long.valueOf(reservationId).equals(r.get("id")))
                .findFirst()
                .map(FinanceUiData::refundRow);
    }

    /** Dòng đầu luôn là tiền cọc, Finance chỉ thu phần còn lại. */
    static List<Map<String, Object>> receiptsOf(Map<String, Object> contract) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(receipt("Tiền cọc", (Long) contract.get("deposit"), "Đặt cọc", String.valueOf(contract.get("reservationCode")),
                String.valueOf(contract.get("created")).substring(0, 10), String.valueOf(contract.get("staff"))));
        rows.addAll(RECEIPTS.getOrDefault((Long) contract.get("id"), List.of()));
        return rows;
    }

    static boolean referenceUsed(String reference) {
        return RECEIPTS.values().stream().flatMap(List::stream).anyMatch(r -> reference.equalsIgnoreCase((String) r.get("reference")))
                || REFUNDS.values().stream().anyMatch(r -> reference.equalsIgnoreCase((String) r.get("reference")));
    }

    /** Thu đủ thì đánh dấu mốc Thanh toán trên hợp đồng, màn Sang tên và Bàn giao đọc được ngay. */
    static void addReceipt(Map<String, Object> contract, long amount, String method, String reference, String staff) {
        long id = (Long) contract.get("id");
        RECEIPTS.computeIfAbsent(id, key -> new CopyOnWriteArrayList<>())
                .add(receipt("Thanh toán", amount, method, reference, LocalDate.now().toString(), staff));
        if ((Long) paymentRow(contract).get("remaining") == 0) {
            SalesDealUiData.markPaid(id);
        }
    }

    static void addRefund(long reservationId, String method, String reference, String note, String staff) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("method", method);
        row.put("reference", reference);
        row.put("note", note);
        row.put("paidOn", LocalDate.now().toString());
        row.put("staff", staff);
        REFUNDS.put(reservationId, row);
    }

    public static List<Map<String, Object>> settlements() {
        return SETTLEMENTS.stream()
                .sorted(Comparator.comparing((Map<String, Object> s) -> (Long) s.get("id")).reversed())
                .toList();
    }

    /** Manager không thấy bản nháp của Finance. */
    public static List<Map<String, Object>> settlementsForReview() {
        return settlements().stream().filter(s -> !DRAFT.equals(s.get("status"))).toList();
    }

    public static Optional<Map<String, Object>> settlement(long id) {
        return SETTLEMENTS.stream().filter(s -> Long.valueOf(id).equals(s.get("id"))).findFirst();
    }

    /** Tiến độ của một quyết toán cho cột dọc trên màn chi tiết. */
    static List<Map<String, String>> settlementSteps(Map<String, Object> row) {
        String status = String.valueOf(row.get("status"));
        boolean approved = APPROVED.equals(status) || PAID_OUT.equals(status);
        boolean sent = approved || PENDING_REVIEW.equals(status);
        return List.of(
                step("Đóng sale", String.valueOf(row.get("contractCode")), true),
                step("Lập quyết toán", String.valueOf(row.get("code")), true),
                step("Gửi Quản lý duyệt", sent ? "Đã gửi" : "Chưa gửi", sent),
                step("Quản lý duyệt", approved ? String.valueOf(row.get("reviewedBy")) : "Chưa duyệt", approved),
                step("Chi trả chủ xe", PAID_OUT.equals(status) ? String.valueOf(row.get("payoutCode")) : "Chưa chi", PAID_OUT.equals(status)));
    }

    private static Map<String, String> step(String label, String note, boolean passed) {
        return Map.of("label", label, "note", note, "passed", String.valueOf(passed));
    }

    /** Đóng sale xong thì quyết toán tự mở ở trạng thái Nháp. */
    public static void openSettlement(Map<String, Object> contract) {
        SETTLEMENTS.add(settlementRow(contract, DRAFT, LocalDateTime.now().withSecond(0).withNano(0).toString()));
    }

    private static Map<String, Object> paymentRow(Map<String, Object> contract) {
        long salePrice = (Long) contract.get("salePrice");
        long collected = receiptsOf(contract).stream().mapToLong(r -> (Long) r.get("amount")).sum();
        long remaining = Math.max(salePrice - collected, 0);
        Map<String, Object> row = base(contract, "payment", String.format("TT-%06d", (Long) contract.get("id")));
        row.put("reference", contract.get("code"));
        row.put("amount", salePrice);
        row.put("collected", collected);
        row.put("remaining", remaining);
        row.put("status", remaining == 0 ? PAID : PARTIAL);
        row.put("created", contract.get("created"));
        return row;
    }

    private static Map<String, Object> refundRow(Map<String, Object> reservation) {
        long id = (Long) reservation.get("id");
        Map<String, Object> done = REFUNDS.get(id);
        Map<String, Object> row = base(reservation, "refund", String.format("HC-%06d", id));
        row.put("reference", reservation.get("code"));
        row.put("amount", reservation.get("deposit"));
        row.put("collected", 0L);
        row.put("remaining", 0L);
        row.put("status", done == null ? REFUND_PENDING : REFUNDED);
        row.put("created", reservation.get("created"));
        row.put("reason", reservation.get("cancelReason"));
        row.put("done", done);
        return row;
    }

    private static Map<String, Object> base(Map<String, Object> source, String kind, String code) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", source.get("id"));
        row.put("kind", kind);
        row.put("code", code);
        for (String key : List.of("buyer", "phone", "car", "plate", "file", "staff", "owner")) {
            row.put(key, source.get(key));
        }
        row.put("contractCode", source.get("code"));
        return row;
    }

    private static Map<String, Object> receipt(String type, long amount, String method, String reference, String paidOn, String staff) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", type);
        row.put("amount", amount);
        row.put("method", method);
        row.put("reference", reference);
        row.put("paidOn", paidOn);
        row.put("staff", staff);
        return row;
    }

    private static Map<String, Object> settlementRow(Map<String, Object> contract, String status, String created) {
        long id = (Long) contract.get("id");
        long salePrice = (Long) contract.get("salePrice");
        long commission = salePrice * COMMISSION_PER_THOUSAND / 1000;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", String.format("QT-%06d", id));
        row.put("contractCode", contract.get("code"));
        for (String key : List.of("car", "plate", "file", "owner", "buyer", "staff")) {
            row.put(key, contract.get(key));
        }
        row.put("salePrice", salePrice);
        row.put("commissionRate", "1,5%");
        row.put("commission", commission);
        row.put("fee", FIXED_FEE);
        row.put("net", salePrice - commission - FIXED_FEE);
        row.put("status", status);
        row.put("created", created);
        row.put("bank", "Vietcombank");
        row.put("account", "****" + String.format("%04d", id));
        row.put("rejectReason", "");
        row.put("reviewedBy", "");
        row.put("payoutCode", String.format("PC-%06d", id));
        row.put("payoutReference", "");
        row.put("paidOn", "");
        return row;
    }

    private static void seed() {
        SalesDealUiData.signedContracts().forEach(contract -> {
            if (Boolean.TRUE.equals(contract.get("paid"))) {
                long id = (Long) contract.get("id");
                long balance = (Long) contract.get("salePrice") - (Long) contract.get("deposit");
                RECEIPTS.computeIfAbsent(id, key -> new CopyOnWriteArrayList<>()).add(receipt("Thanh toán", balance, METHODS.get(0),
                        "CK-" + id, LocalDate.parse((String) contract.get("signedOn")).plusDays(1).toString(), "Mạnh Cường"));
            }
        });
        seedSettlement(201, PAID_OUT);
        seedSettlement(203, APPROVED);
        seedSettlement(204, PENDING_REVIEW);
    }

    private static void seedSettlement(long id, String status) {
        SalesDealUiData.contract(id).ifPresent(contract -> {
            Map<String, Object> row = settlementRow(contract, status, String.valueOf(contract.get("created")));
            if (APPROVED.equals(status) || PAID_OUT.equals(status)) {
                row.put("reviewedBy", "Demo Manager");
            }
            if (PAID_OUT.equals(status)) {
                row.put("payoutReference", "UNC-" + id);
                row.put("paidOn", String.valueOf(contract.get("signedOn")));
            }
            SETTLEMENTS.add(row);
        });
    }

    private static Map<String, Map<String, String>> paymentStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(PARTIAL, status("Còn phải thu", "amber", "coins", "pending"));
        map.put(PAID, status("Đã thu đủ", "green", "check", "listed"));
        map.put(REFUND_PENDING, status("Cần hoàn cọc", "red", "undo", "sold"));
        map.put(REFUNDED, status("Đã hoàn cọc", "slate", "history", "closed"));
        return map;
    }

    private static Map<String, Map<String, String>> settlementStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(DRAFT, status("Nháp", "slate", "pencil", "draft"));
        map.putAll(reviewStatuses());
        return map;
    }

    private static Map<String, Map<String, String>> reviewStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(PENDING_REVIEW, status("Chờ duyệt", "amber", "hourglass", "pending"));
        map.put(APPROVED, status("Đã duyệt", "blue", "shield-check", "signed"));
        map.put(PAID_OUT, status("Đã chi", "green", "check", "listed"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
