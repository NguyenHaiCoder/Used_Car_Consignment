package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

/**
 * Đỗ Lan Anh: dữ liệu mock của luồng Đặt cọc giữ xe và Hợp đồng bán xe, chỉ nằm trong bộ nhớ nên khởi động lại app sẽ về như cũ.
 * Khi nối BE: thay lớp này bằng service đọc Reservation và SalesContract.
 * Chưa có trong entity: quyết định hoàn hay giữ cọc của Reservation; điều khoản và CCCD, địa chỉ người mua của SalesContract
 * (CCCD, địa chỉ nằm ở Buyer); ba mốc paid, transferred, handedOver do luồng Thanh toán, Sang tên, Bàn giao trả về.
 */
public final class SalesDealUiData {

    static final String ACTIVE = "ACTIVE";
    static final String CONVERTED = "CONVERTED";
    static final String EXPIRED = "EXPIRED";
    static final String CANCELLED = "CANCELLED";

    static final String DRAFT = "DRAFT";
    static final String SIGNED = "SIGNED";
    /** Hợp đồng bán đã hoàn tất giao dịch (vẫn còn giá trị pháp lý). */
    static final String COMPLETED = "COMPLETED";

    static final Map<String, Map<String, String>> HOLD_STATUSES = holdStatuses();
    static final Map<String, Map<String, String>> CONTRACT_STATUSES = contractStatuses();
    static final Map<String, String> DEPOSIT_DECISIONS = SalesInquiryUiData.pairs("REFUND", "Hoàn cọc", "KEEP", "Giữ cọc");

    static final String HOLD_TERMS = """
            Bên bán giữ xe cho khách đến hết hạn giữ, không bán cho khách khác trong thời gian này.
            Tiền cọc được trừ vào giá bán khi lập hợp đồng.
            Quá hạn mà chưa lập hợp đồng thì phiếu hết hiệu lực, hai bên thống nhất hoàn hay giữ cọc.""";

    private static final String PAYMENT_TERMS = """
            4.1. Tiền cọc được khấu trừ vào giá bán khi thanh toán.
            4.2. Bên B thanh toán đủ số tiền còn lại trước khi nhận bàn giao xe.
            4.3. Bên A chỉ xác nhận đã nhận tiền khi có chứng từ thanh toán hợp lệ.""";
    private static final String HANDOVER_TERMS = """
            5.1. Bên A bàn giao xe, chìa khóa, phụ kiện và giấy tờ xe sau khi Bên B thanh toán đủ.
            5.2. Hai bên lập biên bản bàn giao, ghi rõ số km, số chìa khóa và phụ kiện kèm theo.
            5.3. Bên A phối hợp với Bên B hoàn tất thủ tục sang tên theo quy định.""";
    private static final String TERMINATION_TERMS = """
            6.1. Hợp đồng hoàn tất khi đủ ba mốc: đã thanh toán, đã sang tên, đã bàn giao.
            6.2. Bên B hủy mua sau khi ký thì tiền cọc được xử lý theo phiếu đặt cọc.
            6.3. Bên A không bàn giao được xe thì hoàn lại toàn bộ số tiền đã nhận.""";

    private static final String LISTED = "LISTED";
    private static final String RESERVED = "RESERVED";
    private static final String SOLD = "SOLD";
    private static final String CONSIGNMENT_COMPLETED = "COMPLETED";
    private static final Map<String, String> CONSIGNMENT_NAMES = SalesInquiryUiData.pairs(
            LISTED, "Đang đăng bán", RESERVED, "Đã đặt cọc", SOLD, "Đã bán", CONSIGNMENT_COMPLETED, "Hoàn thành");

    /** Trạng thái hồ sơ ký gửi của từng xe, chưa có thì coi là Đang đăng bán. Khi nối BE đọc Consignment.status. */
    private static final Map<String, String> CONSIGNMENT_STATUS = new ConcurrentHashMap<>();
    private static final List<Map<String, Object>> RESERVATIONS = new CopyOnWriteArrayList<>();
    private static final List<Map<String, Object>> CONTRACTS = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_RESERVATION = new AtomicLong(123);
    private static final AtomicLong NEXT_CONTRACT = new AtomicLong(214);

    static {
        seedReservations();
        seedContracts();
        seedConsignmentStatus();
    }

    private SalesDealUiData() {
    }

    static List<Map<String, Object>> reservations() {
        refreshHolds();
        return RESERVATIONS;
    }

    static Optional<Map<String, Object>> reservation(long id) {
        return reservations().stream().filter(r -> Long.valueOf(id).equals(r.get("id"))).findFirst();
    }

    /** Phiếu còn hiệu lực, chỉ các phiếu này mới được lập hợp đồng. */
    static List<Map<String, Object>> activeReservations() {
        return reservations().stream().filter(r -> ACTIVE.equals(r.get("status"))).toList();
    }

    /** Phiếu gần nhất của một tư vấn; một tư vấn có thể có phiếu cũ đã hủy hoặc hết hạn. */
    static Optional<Map<String, Object>> latestReservationOf(long inquiryId) {
        return reservations().stream()
                .filter(r -> Long.valueOf(inquiryId).equals(r.get("inquiryId")))
                .max(Comparator.comparing((Map<String, Object> r) -> (Long) r.get("id")));
    }

    static List<Map<String, Object>> contracts() {
        return CONTRACTS;
    }

    /** Hợp đồng đã ký và chưa hủy, là nguồn để Nhân viên hồ sơ tạo hồ sơ sang tên. */
    public static List<Map<String, Object>> signedContracts() {
        return CONTRACTS.stream()
                .filter(c -> !"".equals(c.get("signedOn")) && !CANCELLED.equals(c.get("status")))
                .toList();
    }

    /**
     * Bốn điều kiện để tạo hồ sơ sang tên. Hồ sơ đã bán vẫn tính là đạt, để màn xem của hồ sơ cũ hiện đúng.
     */
    public static List<Map<String, String>> transferChecks(Map<String, Object> contract) {
        Map<String, Object> reservation = reservationRow((Long) contract.get("reservationId"));
        Map<String, Object> inquiry = SalesInquiryUiData.inquiry((Long) contract.get("inquiryId")).orElseThrow();
        boolean deposited = CONVERTED.equals(reservation.get("status"));
        boolean signed = !"".equals(contract.get("signedOn")) && !CANCELLED.equals(contract.get("status"));
        String consignment = consignmentStatus(String.valueOf(contract.get("car")));
        return List.of(
                check("Có khách mua xe", inquiry.get("code") + " · " + inquiry.get("buyer"),
                        SalesInquiryUiData.CONVERTED.equals(inquiry.get("status"))),
                check("Đã đặt cọc giữ xe", reservation.get("code") + " · " + money((Long) reservation.get("deposit")) + " ₫", deposited),
                check("Hợp đồng bán đã ký", String.valueOf(contract.get("code")), signed),
                check("Hồ sơ ký gửi đã đặt cọc", contract.get("file") + " · " + CONSIGNMENT_NAMES.get(consignment),
                        !LISTED.equals(consignment)));
    }

    /** Ba mốc để đóng sale. Thanh toán và bàn giao do luồng khác cập nhật, ở đây chỉ để theo dõi. */
    public static List<Map<String, String>> saleMilestones(Map<String, Object> contract) {
        return List.of(
                milestone("Thanh toán", "Đã thanh toán đủ", "Chưa thanh toán đủ", (Boolean) contract.get("paid")),
                milestone("Sang tên", "Đã sang tên", "Chưa sang tên", (Boolean) contract.get("transferred")),
                milestone("Bàn giao xe", "Đã bàn giao", "Chưa bàn giao", (Boolean) contract.get("handedOver")));
    }

    private static Map<String, String> milestone(String label, String done, String waiting, boolean passed) {
        return check(label, passed ? done : waiting, passed);
    }

    static String consignmentStatus(String car) {
        return CONSIGNMENT_STATUS.getOrDefault(car, LISTED);
    }

    /** Hủy cọc hoặc hết hạn giữ xe thì hồ sơ ký gửi quay lại Đang đăng bán. */
    static void releaseCar(String car) {
        CONSIGNMENT_STATUS.put(car, LISTED);
    }

    /** Đạt đủ bốn điều kiện thì mới được tạo hồ sơ sang tên. */
    public static boolean transferReady(Map<String, Object> contract) {
        return transferChecks(contract).stream().allMatch(c -> "true".equals(c.get("passed")));
    }

    private static Map<String, String> check(String label, String note, boolean passed) {
        return Map.of("label", label, "note", note, "passed", String.valueOf(passed));
    }

    /** Hồ sơ sang tên hoàn tất thì đánh dấu mốc Sang tên trên hợp đồng. */
    public static void markTransferred(long contractId) {
        contract(contractId).ifPresent(c -> c.put("transferred", true));
    }

    /** Khớp theo mã HĐ khi id mock và id DB chưa trùng (cầu nối tạm Sales mock ↔ Sang tên BE). */
    public static void markTransferredByCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        CONTRACTS.stream().filter(c -> code.equals(c.get("code"))).forEach(c -> c.put("transferred", true));
    }

    /** Sales gửi yêu cầu thu: hợp đồng chuyển sang Đang thực hiện và hiện bên Tài chính. */
    static void requestPayment(Map<String, Object> contract) {
        contract.put("paymentRequested", true);
        contract.put("status", ACTIVE);
    }

    public static void markPaid(long contractId) {
        contract(contractId).ifPresent(c -> c.put("paid", true));
    }

    public static void markHandedOver(long contractId) {
        contract(contractId).ifPresent(c -> c.put("handedOver", true));
    }

    /** Đủ ba mốc: hợp đồng hoàn tất, hồ sơ ký gửi thành Đã bán. */
    public static void closeSale(long contractId) {
        contract(contractId).ifPresent(c -> {
            c.put("status", COMPLETED);
            CONSIGNMENT_STATUS.put(String.valueOf(c.get("car")), SOLD);
        });
    }

    /** Chi trả chủ xe xong thì hồ sơ ký gửi hoàn thành. */
    public static void completeConsignment(String car) {
        CONSIGNMENT_STATUS.put(car, CONSIGNMENT_COMPLETED);
    }

    /** Phiếu cọc đã chọn Hoàn cọc, Tài chính xử lý tiếp. */
    public static List<Map<String, Object>> refundableDeposits() {
        return reservations().stream().filter(r -> "REFUND".equals(r.get("depositDecision"))).toList();
    }

    public static Optional<Map<String, Object>> contract(long id) {
        return CONTRACTS.stream().filter(c -> Long.valueOf(id).equals(c.get("id"))).findFirst();
    }

    static Optional<Map<String, Object>> contractOf(long reservationId) {
        return CONTRACTS.stream().filter(c -> Long.valueOf(reservationId).equals(c.get("reservationId"))).findFirst();
    }

    /** Xe đang có phiếu đặt cọc hiệu lực hoặc đã có hợp đồng thì không bán cho khách khác được. */
    static boolean carHeld(String car) {
        return reservations().stream().anyMatch(r -> ACTIVE.equals(r.get("status")) && car.equals(r.get("car")))
                || CONTRACTS.stream().anyMatch(c -> !CANCELLED.equals(c.get("status")) && car.equals(c.get("car")));
    }

    /** Xe còn bán được, lấy từ các hồ sơ ký gửi. */
    static Map<String, Map<String, Object>> availableCars() {
        Map<String, Map<String, Object>> cars = new LinkedHashMap<>();
        SalesInquiryUiData.CARS.forEach((title, car) -> {
            if (!carHeld(title)) {
                cars.put(title, car);
            }
        });
        return cars;
    }

    /** Tư vấn đã có người phụ trách và chưa kết thúc mới đặt cọc được. */
    static boolean canHold(Map<String, Object> inquiry) {
        return List.of(SalesInquiryUiData.OPEN, SalesInquiryUiData.SCHEDULED, SalesInquiryUiData.VIEWED)
                .contains(String.valueOf(inquiry.get("status")));
    }

    static List<Map<String, Object>> inquiriesFor(String car) {
        return SalesInquiryUiData.inquiries().stream()
                .filter(i -> car.equals(i.get("car")))
                .filter(SalesDealUiData::canHold)
                .toList();
    }

    static Map<String, Object> addReservation(Map<String, Object> inquiry, long deposit, String holdUntil, String terms,
            String created) {
        Map<String, Object> row = holdRow(NEXT_RESERVATION.getAndIncrement(), inquiry, ACTIVE, deposit, holdUntil, created);
        row.put("terms", terms);
        RESERVATIONS.add(0, row);
        CONSIGNMENT_STATUS.put(String.valueOf(inquiry.get("car")), RESERVED);
        return row;
    }

    /** Hợp đồng chưa lưu, điền sẵn từ phiếu đặt cọc. */
    static Map<String, Object> draftContract(Map<String, Object> reservation, String created) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", null);
        row.put("code", contractCode(NEXT_CONTRACT.get()));
        row.put("reservationId", reservation.get("id"));
        row.put("reservationCode", reservation.get("code"));
        row.put("inquiryId", reservation.get("inquiryId"));
        row.put("inquiryCode", reservation.get("inquiryCode"));
        for (String key : List.of("buyer", "phone", "email", "car", "file", "plate", "km", "color", "fuel", "owner", "vin", "staff")) {
            row.put(key, reservation.get(key));
        }
        String car = String.valueOf(reservation.get("car"));
        int firstSpace = car.indexOf(' ');
        int lastSpace = car.lastIndexOf(' ');
        row.put("make", car.substring(0, firstSpace));
        row.put("model", car.substring(firstSpace + 1, lastSpace));
        row.put("year", car.substring(lastSpace + 1));
        long deposit = (Long) reservation.get("deposit");
        long price = ((Number) reservation.get("price")).longValue();
        row.put("listedPrice", price);
        row.put("salePrice", price);
        row.put("deposit", deposit);
        row.put("remaining", price - deposit);
        row.put("status", DRAFT);
        row.put("created", created);
        row.put("signedOn", "");
        row.put("buyerIdNumber", "");
        row.put("buyerAddress", "");
        row.put("paymentTerms", PAYMENT_TERMS);
        row.put("handoverTerms", HANDOVER_TERMS);
        row.put("terminationTerms", TERMINATION_TERMS);
        row.put("sellerSignature", "");
        row.put("buyerSignature", "");
        row.put("paid", false);
        row.put("transferred", false);
        row.put("handedOver", false);
        row.put("paymentRequested", false);
        return row;
    }

    /** Lưu hợp đồng từ phiếu đặt cọc; phiếu chuyển sang Đã lập hợp đồng nên không còn hết hạn hay bị hủy nữa. */
    static void addContract(Map<String, Object> contract, Map<String, Object> reservation) {
        long id = NEXT_CONTRACT.getAndIncrement();
        contract.put("id", id);
        contract.put("code", contractCode(id));
        CONTRACTS.add(0, contract);
        reservation.put("status", CONVERTED);
    }

    /** Lọc, phân trang và đưa vào Model các biến mà sales/deal-parts đang đọc. */
    public static void bindList(Model model, String base, List<Map<String, Object>> rows, String amountKey, String dateKey,
            Map<String, Map<String, String>> statuses, Map<String, String> params) {
        String keyword = params.getOrDefault("q", "").trim();
        String lower = keyword.toLowerCase(Locale.ROOT);
        String status = params.getOrDefault("status", "");
        String staff = params.getOrDefault("staff", "");
        String dateFrom = isoDate(params.get("from"));
        String dateTo = isoDate(params.get("to"));
        Long amountMin = million(params.get("amountMin"));
        Long amountMax = million(params.get("amountMax"));

        List<Map<String, Object>> shown = rows.stream()
                .filter(r -> lower.isEmpty() || haystack(r).contains(lower))
                .filter(r -> status.isEmpty() || status.equals(r.get("status")))
                .filter(r -> staff.isEmpty() || staff.equals(r.get("staff")))
                .filter(r -> dateFrom.isEmpty() || day(r, dateKey).compareTo(dateFrom) >= 0)
                .filter(r -> dateTo.isEmpty() || day(r, dateKey).compareTo(dateTo) <= 0)
                .filter(r -> amountMin == null || (Long) r.get(amountKey) >= amountMin)
                .filter(r -> amountMax == null || (Long) r.get(amountKey) <= amountMax)
                .toList();

        Integer size = number(params.get("size"));
        Integer wanted = number(params.get("page"));
        Page<Map<String, Object>> page = CatalogPageSupport.pageOf(shown, wanted == null ? 0 : wanted, size);

        Map<String, Long> counts = new LinkedHashMap<>();
        statuses.keySet().forEach(key -> counts.put(key, rows.stream().filter(r -> key.equals(r.get("status"))).count()));

        model.addAttribute("base", base);
        model.addAttribute("page", page);
        model.addAttribute("counts", counts);
        model.addAttribute("staffs", rows.stream().map(r -> String.valueOf(r.get("staff"))).filter(s -> !s.isBlank())
                .distinct().sorted().toList());
        model.addAttribute("q", keyword);
        model.addAttribute("status", status);
        model.addAttribute("staff", staff);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("amountMin", amountMin == null ? "" : params.get("amountMin"));
        model.addAttribute("amountMax", amountMax == null ? "" : params.get("amountMax"));
        model.addAttribute("size", CatalogPageSupport.normalizeSize(size));
    }

    public static String money(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.');
    }

    /** Số tiền nhập theo dạng 76.000.000. Không nhập hoặc nhập sai trả về null. */
    public static Long parseMoney(String text) {
        String digits = text == null ? "" : text.replaceAll("\\D", "");
        return digits.isEmpty() || digits.length() > 13 ? null : Long.valueOf(digits);
    }

    public static Long parseId(String text) {
        return text == null || !text.matches("\\d{1,9}") ? null : Long.valueOf(text);
    }

    public static LocalDate parseDate(String text) {
        return isoDate(text).isEmpty() ? null : LocalDate.parse(text);
    }

    /** Hết hạn giữ mà chưa lập hợp đồng thì xe được mở bán lại và tư vấn quay về bước trước đó. */
    private static void refreshHolds() {
        LocalDate today = LocalDate.now();
        for (Map<String, Object> row : RESERVATIONS) {
            LocalDate holdUntil = LocalDate.parse((String) row.get("holdUntil"));
            row.put("daysLeft", ChronoUnit.DAYS.between(today, holdUntil));
            if (ACTIVE.equals(row.get("status")) && holdUntil.isBefore(today)) {
                row.put("status", EXPIRED);
                releaseCar(String.valueOf(row.get("car")));
                SalesInquiryUiData.inquiry((Long) row.get("inquiryId")).ifPresent(i -> i.put("status",
                        SalesInquiryUiData.statusWithoutActiveStep((Long) i.get("id"))));
            }
        }
    }

    private static String haystack(Map<String, Object> row) {
        return String.join(" ", List.of("code", "contractCode", "reservationCode", "file", "car", "plate", "buyer", "phone").stream()
                .map(row::get).filter(Objects::nonNull).map(String::valueOf).toList()).toLowerCase(Locale.ROOT);
    }

    private static String day(Map<String, Object> row, String dateKey) {
        return String.valueOf(row.get(dateKey)).substring(0, 10);
    }

    private static String isoDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "";
        }
        try {
            LocalDate.parse(text);
            return text;
        } catch (java.time.format.DateTimeParseException ex) {
            return "";
        }
    }

    private static Long million(String text) {
        Integer value = number(text);
        return value == null ? null : value * 1_000_000L;
    }

    private static Integer number(String text) {
        return text == null || !text.matches("\\d{1,5}") ? null : Integer.valueOf(text);
    }

    private static String contractCode(long id) {
        return String.format("HDBX-%06d", id);
    }

    private static void seedReservations() {
        hold(122, 1224, ACTIVE, 70, 5, "2026-11-05T09:20");
        hold(121, 1203, ACTIVE, 55, 3, "2026-11-04T15:40");
        hold(120, 1202, ACTIVE, 60, 1, "2026-11-03T10:15");
        hold(119, 1201, ACTIVE, 70, 6, "2026-11-02T14:05");
        hold(118, 1200, ACTIVE, 65, 0, "2026-10-31T09:50");
        hold(117, 1216, CONVERTED, 95, 7, "2026-10-30T16:20");
        hold(116, 1215, CONVERTED, 75, 7, "2026-10-29T11:30");
        hold(115, 1214, CONVERTED, 65, 7, "2026-10-28T10:10");
        hold(114, 1213, CONVERTED, 130, 7, "2026-10-27T15:00");
        hold(113, 1212, CONVERTED, 60, 7, "2026-10-26T09:40");
        hold(112, 1211, CONVERTED, 80, 7, "2026-10-25T14:30");
        hold(111, 1210, CONVERTED, 100, 7, "2026-10-24T10:45");
        hold(110, 1209, CONVERTED, 85, 7, "2026-10-23T16:10");
        hold(109, 1208, CONVERTED, 60, 7, "2026-10-22T09:15");
        hold(108, 1199, EXPIRED, 90, 7, "2026-10-21T13:20");
        hold(107, 1198, EXPIRED, 75, 5, "2026-10-20T10:00");
        hold(106, 1207, CONVERTED, 100, 7, "2026-10-19T15:25");
        hold(105, 1206, CONVERTED, 115, 7, "2026-10-18T11:50");
        hold(104, 1197, CANCELLED, 85, 7, "2026-10-17T09:35");
        hold(103, 1205, CONVERTED, 65, 7, "2026-10-16T14:15");
        hold(102, 1196, CANCELLED, 40, 5, "2026-10-15T16:40");
        hold(101, 1204, CONVERTED, 45, 7, "2026-10-14T10:20");

        reservationRow(107).put("depositDecision", "REFUND");
        Map<String, Object> changedMind = reservationRow(104);
        changedMind.put("cancelReason", "Khách đổi ý, chuyển sang mua xe khác.");
        changedMind.put("depositDecision", "KEEP");
        changedMind.put("cancelledAt", "2026-10-20T10:30");
        Map<String, Object> noLoan = reservationRow(102);
        noLoan.put("cancelReason", "Ngân hàng không duyệt khoản vay của khách.");
        noLoan.put("depositDecision", "REFUND");
        noLoan.put("cancelledAt", "2026-10-18T09:10");
    }

    private static void seedContracts() {
        contract(213, 117, DRAFT, "2026-11-05T10:30", false, false, false);
        contract(212, 116, DRAFT, "2026-11-04T09:20", false, false, false);
        contract(211, 115, DRAFT, "2026-11-02T14:50", false, false, false);
        contract(210, 114, SIGNED, "2026-10-30T10:05", false, false, false);
        contract(209, 113, SIGNED, "2026-10-29T15:30", false, false, false);
        contract(208, 112, SIGNED, "2026-10-28T09:45", false, false, false);
        contract(207, 111, ACTIVE, "2026-10-27T11:20", true, false, false);
        contract(206, 110, ACTIVE, "2026-10-26T14:10", true, true, false);
        contract(205, 109, ACTIVE, "2026-10-25T10:35", true, false, false);
        contract(204, 106, COMPLETED, "2026-10-21T09:30", true, true, true);
        contract(203, 105, COMPLETED, "2026-10-20T15:15", true, true, true);
        contract(202, 103, CANCELLED, "2026-10-18T10:40", false, false, false);
        contract(201, 101, COMPLETED, "2026-10-16T13:55", true, true, true);
    }

    private static void seedConsignmentStatus() {
        RESERVATIONS.stream()
                .filter(r -> ACTIVE.equals(r.get("status")) || CONVERTED.equals(r.get("status")))
                .forEach(r -> CONSIGNMENT_STATUS.put(String.valueOf(r.get("car")), RESERVED));
        CONTRACTS.forEach(c -> {
            String car = String.valueOf(c.get("car"));
            if (COMPLETED.equals(c.get("status"))) {
                CONSIGNMENT_STATUS.put(car, SOLD);
            } else if (CANCELLED.equals(c.get("status"))) {
                releaseCar(car);
            }
        });
    }

    private static void hold(long id, long inquiryId, String status, long depositMillion, int holdDays, String created) {
        Map<String, Object> inquiry = SalesInquiryUiData.inquiry(inquiryId).orElseThrow();
        LocalDate from = ACTIVE.equals(status) ? LocalDate.now() : LocalDate.parse(created.substring(0, 10));
        RESERVATIONS.add(holdRow(id, inquiry, status, depositMillion * 1_000_000L, from.plusDays(holdDays).toString(), created));
    }

    private static Map<String, Object> holdRow(long id, Map<String, Object> inquiry, String status, long deposit,
            String holdUntil, String created) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", String.format("DC-%06d", id));
        row.put("inquiryId", inquiry.get("id"));
        row.put("inquiryCode", inquiry.get("code"));
        row.put("ini", inquiry.get("ini"));
        for (String key : List.of("buyer", "phone", "email", "car", "file", "plate", "km", "price", "color", "fuel", "owner", "vin", "staff")) {
            row.put(key, inquiry.get(key));
        }
        row.put("status", status);
        row.put("deposit", deposit);
        row.put("holdUntil", holdUntil);
        row.put("daysLeft", 0L);
        row.put("terms", HOLD_TERMS);
        row.put("created", created);
        row.put("cancelReason", "");
        row.put("cancelledAt", "");
        row.put("depositDecision", "");
        return row;
    }

    private static Map<String, Object> reservationRow(long id) {
        return RESERVATIONS.stream().filter(r -> Long.valueOf(id).equals(r.get("id"))).findFirst().orElseThrow();
    }

    private static void contract(long id, long reservationId, String status, String created, boolean paid,
            boolean transferred, boolean handedOver) {
        Map<String, Object> row = draftContract(reservationRow(reservationId), created);
        row.put("id", id);
        row.put("code", contractCode(id));
        row.put("status", status);
        row.put("paid", paid);
        row.put("transferred", transferred);
        row.put("handedOver", handedOver);
        row.put("paymentRequested", paid || id == 208);
        if (!DRAFT.equals(status)) {
            row.put("signedOn", created.substring(0, 10));
            row.put("buyerIdNumber", String.format("0790%08d", id * 137));
            row.put("buyerAddress", "Cầu Giấy, Hà Nội");
        }
        CONTRACTS.add(row);
    }

    private static Map<String, Map<String, String>> holdStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(ACTIVE, status("Đang giữ xe", "amber", "coins", "pending"));
        map.put(CONVERTED, status("Đã lập hợp đồng", "green", "handshake", "listed"));
        map.put(EXPIRED, status("Hết hạn giữ", "slate", "clock", "closed"));
        map.put(CANCELLED, status("Đã hủy", "red", "close", "sold"));
        return map;
    }

    private static Map<String, Map<String, String>> contractStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(DRAFT, status("Lưu nháp", "slate", "pencil", "draft"));
        map.put(SIGNED, status("Đã ký", "blue", "handshake", "signed"));
        map.put(ACTIVE, status("Đang thực hiện", "amber", "hourglass", "pending"));
        map.put(COMPLETED, status("Hoàn tất", "green", "check", "listed"));
        map.put(CANCELLED, status("Đã hủy", "red", "close", "sold"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
