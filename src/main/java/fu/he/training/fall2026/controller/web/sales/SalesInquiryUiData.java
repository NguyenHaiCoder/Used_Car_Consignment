package fu.he.training.fall2026.controller.web.sales;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Đỗ Lan Anh: dữ liệu mock của luồng Tư vấn &amp; lịch xem xe, chỉ nằm trong bộ nhớ nên khởi động lại app sẽ về như cũ.
 * Khi nối BE: thay lớp này bằng service đọc Buyer, Inquiry, Viewing.
 * Các khóa map trùng tên biến mà template đang đọc.
 */
final class SalesInquiryUiData {

    static final String NEW = "NEW";
    static final String OPEN = "OPEN";
    static final String SCHEDULED = "SCHEDULED";
    static final String VIEWED = "VIEWED";
    static final String CONVERTED = "CONVERTED";
    static final String CLOSED = "CLOSED";

    static final String BOOKED = "BOOKED";
    static final String CONFIRMED = "CONFIRMED";
    static final String DONE = "DONE";
    static final String CANCELLED = "CANCELLED";

    /** NEW không phải enum InquiryStatus: là Inquiry OPEN chưa có người phụ trách. */
    static final Map<String, Map<String, String>> STATUSES = statuses();

    static final Map<String, String> VIEWING_NAMES = pairs(
            BOOKED, "Đã đặt", CONFIRMED, "Đã xác nhận", DONE, "Đã xem", CANCELLED, "Đã hủy");
    static final Map<String, String> VIEWING_PILLS = pairs(
            BOOKED, "pending", CONFIRMED, "approved", DONE, "closed", CANCELLED, "closed-red");
    static final Map<String, String> CHANNELS = pairs(
            "WEBSITE", "Website", "PHONE", "Điện thoại", "SHOWROOM", "Tại showroom");
    static final Map<String, String> NEEDS = pairs(
            "PRICE", "Hỏi giá", "VIEWING", "Xem xe", "TEST_DRIVE", "Lái thử");
    static final Map<String, String> VIEWING_TYPES = pairs(
            "VIEWING", "Xem xe", "TEST_DRIVE", "Lái thử");

    static final List<String> PLACES = List.of("Showroom Hòa Lạc", "Showroom Cầu Giấy");
    static final List<String> CLOSE_REASONS = List.of(
            "Không quan tâm", "Đã mua xe khác", "Không liên lạc được", "Ngân sách không phù hợp", "Lý do khác");
    static final List<String> SLOTS = slots();

    static final Map<String, Map<String, Object>> CARS = cars();

    private static final List<Map<String, Object>> INQUIRIES = new CopyOnWriteArrayList<>();
    private static final List<Map<String, Object>> VIEWINGS = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_INQUIRY = new AtomicLong(1241);
    private static final AtomicLong NEXT_VIEWING = new AtomicLong(13);

    static {
        seedInquiries();
        seedViewings();
    }

    private SalesInquiryUiData() {
    }

    static List<Map<String, Object>> inquiries() {
        return INQUIRIES;
    }

    static Optional<Map<String, Object>> inquiry(long id) {
        return INQUIRIES.stream().filter(i -> Long.valueOf(id).equals(i.get("id"))).findFirst();
    }

    static List<Map<String, Object>> viewingsOf(long inquiryId) {
        return VIEWINGS.stream()
                .filter(v -> Long.valueOf(inquiryId).equals(v.get("inquiryId")))
                .sorted(Comparator.comparing((Map<String, Object> v) -> (Long) v.get("id")).reversed())
                .toList();
    }

    /** Lịch mới nhất của từng tư vấn, dùng cho cột Lịch xem. */
    static Map<Long, Map<String, Object>> latestViewings() {
        Map<Long, Map<String, Object>> latest = new LinkedHashMap<>();
        for (Map<String, Object> inquiry : INQUIRIES) {
            Long id = (Long) inquiry.get("id");
            List<Map<String, Object>> rows = viewingsOf(id);
            if (!rows.isEmpty()) {
                latest.put(id, rows.get(0));
            }
        }
        return latest;
    }

    static Optional<Map<String, Object>> viewing(long inquiryId, long viewingId) {
        return viewingsOf(inquiryId).stream().filter(v -> Long.valueOf(viewingId).equals(v.get("id"))).findFirst();
    }

    static boolean hasActiveViewing(long inquiryId) {
        return viewingsOf(inquiryId).stream().anyMatch(SalesInquiryUiData::isActive);
    }

    /** Trạng thái quay về khi lịch xem hoặc phiếu đặt cọc kết thúc: khách đã từng xem xe thì giữ Đã xem xe. */
    static String statusWithoutActiveStep(long inquiryId) {
        return viewingsOf(inquiryId).stream().anyMatch(v -> DONE.equals(v.get("status"))) ? VIEWED : OPEN;
    }

    static boolean isActive(Map<String, Object> viewing) {
        return BOOKED.equals(viewing.get("status")) || CONFIRMED.equals(viewing.get("status"));
    }

    /** Một xe không thể có hai lịch trùng giờ, kể cả với khách khác. */
    static boolean carBusy(String car, LocalDateTime start, LocalDateTime end, Long exceptViewingId) {
        return VIEWINGS.stream()
                .filter(SalesInquiryUiData::isActive)
                .filter(v -> !v.get("id").equals(exceptViewingId))
                .filter(v -> inquiry((Long) v.get("inquiryId")).map(i -> car.equals(i.get("car"))).orElse(false))
                .anyMatch(v -> {
                    LocalDateTime otherStart = LocalDateTime.parse((String) v.get("start"));
                    LocalDateTime otherEnd = otherStart.plusMinutes((Integer) v.get("minutes"));
                    return start.isBefore(otherEnd) && otherStart.isBefore(end);
                });
    }

    static Optional<Map<String, Object>> buyerByPhone(String phone) {
        String wanted = digits(phone);
        return wanted.isEmpty()
                ? Optional.empty()
                : INQUIRIES.stream().filter(i -> digits((String) i.get("phone")).equals(wanted)).findFirst();
    }

    static Optional<Map<String, Object>> openInquiryOf(String phone, String car) {
        String wanted = digits(phone);
        return INQUIRIES.stream()
                .filter(i -> digits((String) i.get("phone")).equals(wanted) && car.equals(i.get("car")))
                .filter(i -> !CLOSED.equals(i.get("status")) && !CONVERTED.equals(i.get("status")))
                .findFirst();
    }

    static Map<String, Object> addInquiry(Map<String, Object> values) {
        long id = NEXT_INQUIRY.getAndIncrement();
        Map<String, Object> row = new LinkedHashMap<>(values);
        row.put("id", id);
        row.put("code", String.format("TV-%06d", id));
        row.put("ini", initials((String) values.get("buyer")));
        row.put("staffIni", initials((String) values.get("staff")));
        row.put("closeReason", "");
        row.putAll(CARS.get(values.get("car")));
        INQUIRIES.add(0, row);
        return row;
    }

    static Map<String, Object> addViewing(long inquiryId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", NEXT_VIEWING.getAndIncrement());
        row.put("inquiryId", inquiryId);
        VIEWINGS.add(row);
        return row;
    }

    static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] words = name.trim().split("\\s+");
        String last = words[words.length - 1].substring(0, 1);
        return words.length == 1 ? last.toUpperCase() : (words[words.length - 2].charAt(0) + last).toUpperCase();
    }

    static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    private static void seedInquiries() {
        String lan = "Đỗ Lan Anh";
        String khoi = "Trần Minh Khôi";
        String ha = "Lê Thu Hà";
        seed(1240, "Lê Quốc Bảo", "0903 112 233", "Toyota Camry 2.0G 2020", NEW, "WEBSITE", "TEST_DRIVE", "", "2026-11-06T15:20", "Khách muốn lái thử vào cuối tuần.");
        seed(1239, "Đinh Thu Trang", "0987 450 126", "Mazda CX-5 2.0 Luxury 2021", NEW, "WEBSITE", "VIEWING", "", "2026-11-06T11:05", "");
        seed(1238, "Hoàng Văn Sơn", "0934 208 771", "Honda City RS 2022", NEW, "WEBSITE", "PRICE", "", "2026-11-06T09:40", "Hỏi giá lăn bánh và có thương lượng không.");
        seed(1237, "Mai Phương Thảo", "0918 662 045", "Toyota Vios 1.5G 2021", NEW, "WEBSITE", "PRICE", "", "2026-11-05T20:15", "");
        Map<String, Object> nam = seed(1236, "Trần Hoàng Nam", "0912 345 678", "Hyundai Tucson 2.0 2022", SCHEDULED, "WEBSITE", "VIEWING", lan, "2026-11-05T10:32", "Khách quan tâm xe gia đình, ưu tiên xe còn bảo hành, muốn xem và lái thử.");
        nam.putAll(Map.of("email", "nam.tran@gmail.com", "area", "Hà Nội – Cầu Giấy",
                "buyerNote", "Khách ưu tiên xe SUV, sử dụng cho gia đình 4 người.",
                "budgetMin", "750", "budgetMax", "780", "priorities", List.of("Màu xanh", "Xe gia đình"),
                "buyTime", "Trong tháng 11/2026"));
        Map<String, Object> lanNguyen = seed(1235, "Nguyễn Lan", "0936 112 233", "Toyota Camry 2.0G 2020", SCHEDULED, "PHONE", "VIEWING", lan, "2026-11-05T09:10", "Hẹn xem xe cùng chồng.");
        lanNguyen.putAll(Map.of("area", "Hà Nội – Hoàn Kiếm", "budgetMin", "880", "budgetMax", "930", "buyTime", "Trong tháng 12/2026"));
        seed(1234, "Phạm Minh Tú", "0988 765 432", "Mazda CX-5 2.0 Luxury 2021", OPEN, "WEBSITE", "PRICE", lan, "2026-11-04T16:45", "Đang so sánh với CR-V.");
        seed(1233, "Vũ Đức Duy", "0977 889 900", "Honda CR-V 1.5L 2022", VIEWED, "SHOWROOM", "TEST_DRIVE", lan, "2026-11-03T14:00", "Đã lái thử, hài lòng, đang cân nhắc đặt cọc.");
        seed(1232, "Ngô Hải Yến", "0904 556 778", "Kia Seltos 1.6 Premium 2023", CLOSED, "WEBSITE", "PRICE", lan, "2026-11-03T10:20", "Khách đã mua xe ở nơi khác.").put("closeReason", "Đã mua xe khác");
        seed(1231, "Bùi Tuấn Kiệt", "0962 300 418", "Ford Ranger Wildtrak 2020", OPEN, "PHONE", "PRICE", khoi, "2026-11-03T08:55", "Cần xe chở hàng, hỏi về lịch sử bảo dưỡng.");
        seed(1230, "Đặng Anh Thư", "0945 871 206", "VinFast VF 8 Plus 2023", SCHEDULED, "WEBSITE", "TEST_DRIVE", khoi, "2026-11-02T17:30", "Muốn lái thử quãng đường dài.");
        seed(1229, "Tạ Hồng Loan", "0978 124 590", "Mitsubishi Xpander 1.5 AT 2023", SCHEDULED, "SHOWROOM", "VIEWING", lan, "2026-11-02T10:05", "Gia đình 6 người, cần xe 7 chỗ.");
        seed(1228, "Lý Quang Vinh", "0931 760 824", "Hyundai Accent 1.4 AT 2022", OPEN, "WEBSITE", "PRICE", ha, "2026-11-01T19:40", "");
        seed(1227, "Cao Ngọc Anh", "0909 417 362", "Toyota Fortuner 2.4 AT 2021", VIEWED, "PHONE", "VIEWING", ha, "2026-11-01T09:15", "Muốn giảm giá thêm 20 triệu.");
        seed(1226, "Hồ Đức Hải", "0917 538 640", "Ford Ranger Wildtrak 2020", SCHEDULED, "WEBSITE", "TEST_DRIVE", khoi, "2026-10-31T15:25", "");
        seed(1225, "Phan Minh Châu", "0983 629 157", "Honda City RS 2022", OPEN, "SHOWROOM", "PRICE", lan, "2026-10-30T11:50", "Đang chờ người nhà góp ý.");
        seed(1224, "Võ Thanh Phong", "0926 481 305", "Kia Seltos 1.6 Premium 2023", CONVERTED, "WEBSITE", "VIEWING", ha, "2026-10-29T14:35", "Đã đặt cọc giữ xe.");
        seed(1223, "Kiều Thanh Hương", "0937 205 816", "Toyota Vios 1.5G 2021", SCHEDULED, "PHONE", "VIEWING", khoi, "2026-10-29T09:00", "");
        seed(1222, "Tô Gia Bảo", "0968 742 093", "Hyundai Tucson 2.0 2022", OPEN, "WEBSITE", "PRICE", ha, "2026-10-28T20:10", "");
        seed(1221, "Dương Khánh Linh", "0912 906 738", "Mazda CX-5 2.0 Luxury 2021", VIEWED, "WEBSITE", "TEST_DRIVE", lan, "2026-10-27T10:45", "Muốn xem thêm xe màu khác.");
        seed(1220, "Châu Văn Tài", "0946 358 201", "Honda CR-V 1.5L 2022", OPEN, "PHONE", "PRICE", khoi, "2026-10-26T16:30", "");
        seed(1219, "Thái Bảo Ngọc", "0975 213 864", "Toyota Fortuner 2.4 AT 2021", CLOSED, "WEBSITE", "VIEWING", ha, "2026-10-25T09:20", "Khách hủy lịch.").put("closeReason", "Không liên lạc được");
        seed(1218, "Lâm Hữu Phước", "0902 687 345", "Mitsubishi Xpander 1.5 AT 2023", VIEWED, "SHOWROOM", "VIEWING", lan, "2026-10-24T14:15", "");
        seed(1217, "Nghiêm Tú Linh", "0935 470 912", "VinFast VF 8 Plus 2023", OPEN, "WEBSITE", "PRICE", khoi, "2026-10-23T11:00", "");
        seed(1216, "Trịnh Hoài Nam", "0913 220 145", "Kia Sorento 2.2D 2022", CONVERTED, "WEBSITE", "VIEWING", lan, "2026-10-22T09:30", "");
        seed(1215, "Nguyễn Thị Bích", "0986 114 372", "Honda Civic RS 2022", CONVERTED, "PHONE", "PRICE", ha, "2026-10-22T08:15", "");
        seed(1214, "Phùng Văn Tuấn", "0935 208 664", "Toyota Innova 2.0E 2021", CONVERTED, "SHOWROOM", "VIEWING", khoi, "2026-10-21T16:40", "");
        seed(1213, "Hà Minh Quân", "0907 341 228", "Kia Carnival 2.2D 2022", CONVERTED, "WEBSITE", "TEST_DRIVE", lan, "2026-10-21T10:05", "");
        seed(1212, "Lưu Thanh Mai", "0972 660 913", "Mazda 3 1.5 Luxury 2021", CONVERTED, "PHONE", "VIEWING", ha, "2026-10-20T14:50", "");
        seed(1211, "Đoàn Quốc Việt", "0916 503 847", "Toyota Corolla Cross 1.8V 2023", CONVERTED, "WEBSITE", "PRICE", khoi, "2026-10-20T09:20", "");
        seed(1210, "Trương Thị Ngân", "0944 278 106", "Ford Everest 2.0L 2021", CONVERTED, "SHOWROOM", "TEST_DRIVE", lan, "2026-10-19T15:35", "");
        seed(1209, "Lại Đức Anh", "0963 815 402", "Peugeot 3008 1.6 AT 2021", CONVERTED, "WEBSITE", "VIEWING", ha, "2026-10-18T11:10", "");
        seed(1208, "Mạc Thu Hiền", "0908 926 531", "Hyundai Elantra 1.6 AT 2022", CONVERTED, "PHONE", "PRICE", khoi, "2026-10-17T09:45", "");
        seed(1207, "Vương Gia Huy", "0989 137 620", "Hyundai Santa Fe 2.2D 2021", CONVERTED, "SHOWROOM", "VIEWING", lan, "2026-10-15T14:00", "");
        seed(1206, "Quách Ngọc Trâm", "0932 470 158", "Mercedes-Benz C200 2020", CONVERTED, "WEBSITE", "TEST_DRIVE", ha, "2026-10-14T10:30", "");
        seed(1205, "Giang Văn Hoàng", "0919 382 744", "Mazda 6 2.0 Premium 2020", CONVERTED, "PHONE", "PRICE", khoi, "2026-10-13T16:20", "");
        seed(1204, "Ông Thị Thu", "0976 251 083", "Hyundai Accent 1.4 AT 2022", CONVERTED, "WEBSITE", "VIEWING", lan, "2026-10-12T09:05", "");
        seed(1203, "Nhâm Thị Yến", "0905 618 247", "Suzuki XL7 1.5 AT 2022", CONVERTED, "WEBSITE", "VIEWING", lan, "2026-10-30T15:10", "");
        seed(1202, "Tăng Minh Đạt", "0947 903 562", "Kia K3 1.6 Premium 2022", CONVERTED, "SHOWROOM", "TEST_DRIVE", khoi, "2026-10-30T10:25", "");
        seed(1201, "Lê Hồng Nhung", "0981 340 729", "VinFast Lux A2.0 2021", CONVERTED, "PHONE", "PRICE", ha, "2026-10-29T16:50", "");
        seed(1200, "Bạch Văn Quý", "0923 574 816", "Nissan Navara 2.5 AT 2021", CONVERTED, "WEBSITE", "VIEWING", lan, "2026-10-28T11:35", "");
        seed(1199, "Mã Thanh Tùng", "0914 762 305", "Toyota Camry 2.0G 2020", OPEN, "PHONE", "VIEWING", khoi, "2026-10-18T09:10", "");
        seed(1198, "Thân Thị Hạnh", "0967 283 941", "Mazda CX-5 2.0 Luxury 2021", OPEN, "WEBSITE", "PRICE", ha, "2026-10-16T14:25", "");
        seed(1197, "Diệp Bảo Long", "0938 120 476", "Ford Ranger Wildtrak 2020", OPEN, "SHOWROOM", "TEST_DRIVE", lan, "2026-10-15T10:40", "");
        seed(1196, "Sầm Ngọc Diệp", "0902 947 613", "Toyota Vios 1.5G 2021", OPEN, "WEBSITE", "PRICE", khoi, "2026-10-14T15:55", "");
    }

    private static void seedViewings() {
        viewing(1, 1236, "2026-11-08T14:00", 45, "Showroom Hòa Lạc", "VIEWING", CONFIRMED);
        viewing(2, 1235, "2026-11-09T10:30", 45, "Showroom Hòa Lạc", "VIEWING", BOOKED);
        viewing(3, 1233, "2026-11-05T15:00", 45, "Showroom Cầu Giấy", "TEST_DRIVE", DONE);
        viewing(4, 1230, "2026-11-10T09:00", 45, "Showroom Hòa Lạc", "TEST_DRIVE", BOOKED);
        viewing(5, 1229, "2026-11-07T16:00", 45, "Showroom Cầu Giấy", "VIEWING", CONFIRMED);
        viewing(6, 1227, "2026-11-04T10:00", 45, "Showroom Hòa Lạc", "VIEWING", DONE);
        viewing(7, 1226, "2026-11-08T09:30", 45, "Showroom Cầu Giấy", "TEST_DRIVE", CONFIRMED);
        viewing(8, 1224, "2026-11-02T14:30", 45, "Showroom Hòa Lạc", "VIEWING", DONE);
        viewing(9, 1223, "2026-11-11T15:30", 45, "Showroom Hòa Lạc", "VIEWING", BOOKED);
        viewing(10, 1221, "2026-10-31T09:00", 45, "Showroom Cầu Giấy", "TEST_DRIVE", DONE);
        viewing(11, 1219, "2026-10-29T14:00", 45, "Showroom Hòa Lạc", "VIEWING", CANCELLED);
        viewing(12, 1218, "2026-10-28T10:00", 45, "Showroom Hòa Lạc", "VIEWING", DONE);
    }

    private static Map<String, Object> seed(long id, String buyer, String phone, String car, String status,
            String channel, String need, String staff, String created, String note) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("code", String.format("TV-%06d", id));
        row.put("ini", initials(buyer));
        row.put("buyer", buyer);
        row.put("phone", phone);
        row.put("email", "");
        row.put("area", "");
        row.put("buyerNote", "");
        row.put("car", car);
        row.putAll(CARS.get(car));
        row.put("status", status);
        row.put("channel", channel);
        row.put("need", need);
        row.put("staff", staff);
        row.put("staffIni", initials(staff));
        row.put("created", created);
        row.put("note", note);
        row.put("budgetMin", "");
        row.put("budgetMax", "");
        row.put("priorities", List.of());
        row.put("buyTime", "");
        row.put("closeReason", "");
        INQUIRIES.add(row);
        return row;
    }

    private static void viewing(long id, long inquiryId, String start, int minutes, String place, String type, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("inquiryId", inquiryId);
        row.put("start", start);
        row.put("minutes", minutes);
        row.put("end", LocalDateTime.parse(start).plusMinutes(minutes).toLocalTime().toString());
        row.put("place", place);
        row.put("typeKey", type);
        row.put("type", VIEWING_TYPES.get(type));
        row.put("status", status);
        row.put("note", "");
        VIEWINGS.add(row);
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(NEW, status("Yêu cầu mới", "violet", "bell", "new"));
        map.put(OPEN, status("Đang tư vấn", "blue", "chat", "open"));
        map.put(SCHEDULED, status("Đã đặt lịch xem", "amber", "calendar-check", "scheduled"));
        map.put(VIEWED, status("Đã xem xe", "green", "check", "viewed"));
        map.put(CONVERTED, status("Đã đặt cọc", "green", "coins", "converted"));
        map.put(CLOSED, status("Đóng tư vấn", "red", "close", "closed-red"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static Map<String, Map<String, Object>> cars() {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        car(map, "Hyundai Tucson 2.0 2022", 769000000, 38650, "Xăng", "Xanh dương", "KG-001205", "51H-678.90", "Nguyễn Quang Huy");
        car(map, "Toyota Camry 2.0G 2020", 920000000, 62100, "Xăng", "Đen", "KG-001204", "30G-456.78", "Trần Thị Lan");
        car(map, "Mazda CX-5 2.0 Luxury 2021", 760000000, 45200, "Xăng", "Đỏ", "KG-001203", "30K-123.45", "Lê Thị Mai");
        car(map, "Honda CR-V 1.5L 2022", 1080000000, 28900, "Xăng", "Xám", "KG-001202", "51K-789.01", "Đỗ Văn Nam");
        car(map, "Kia Seltos 1.6 Premium 2023", 680000000, 51300, "Xăng", "Trắng", "KG-001201", "30H-678.22", "Phạm Thu Trang");
        car(map, "Ford Ranger Wildtrak 2020", 845000000, 41800, "Dầu", "Cam", "KG-001200", "51D-345.12", "Phạm Quốc Huy");
        car(map, "Toyota Vios 1.5G 2021", 425000000, 54200, "Xăng", "Trắng", "KG-001199", "30A-882.16", "Vũ Thị Hồng");
        car(map, "Mitsubishi Xpander 1.5 AT 2023", 610000000, 21300, "Xăng", "Bạc", "KG-001196", "51K-564.09", "Ngô Thị Lan");
        car(map, "VinFast VF 8 Plus 2023", 890000000, 15600, "Điện", "Đen", "KG-001195", "30K-903.28", "Hoàng Anh Tuấn");
        car(map, "Honda City RS 2022", 535000000, 18700, "Xăng", "Trắng", "KG-001197", "30L-117.35", "Bùi Thanh Tùng");
        car(map, "Hyundai Accent 1.4 AT 2022", 465000000, 33100, "Xăng", "Trắng", "KG-001192", "51G-118.73", "Mai Hồng Sơn");
        car(map, "Toyota Fortuner 2.4 AT 2021", 1020000000, 58400, "Dầu", "Bạc", "KG-001193", "30G-742.50", "Lê Văn Khoa");
        car(map, "Kia Sorento 2.2D 2022", 950000000, 36300, "Dầu", "Trắng", "KG-001198", "51L-220.47", "Trần Văn Bình");
        car(map, "Honda Civic RS 2022", 745000000, 35600, "Xăng", "Trắng", "KG-001187", "30G-391.84", "Phan Ngọc Ánh");
        car(map, "Toyota Innova 2.0E 2021", 655000000, 72500, "Xăng", "Bạc", "KG-001182", "51H-947.33", "Lâm Văn Phúc");
        car(map, "Kia Carnival 2.2D 2022", 1280000000, 40500, "Dầu", "Đen", "KG-001191", "30K-276.91", "Dương Thị Hoa");
        car(map, "Mazda 3 1.5 Luxury 2021", 585000000, 47900, "Xăng", "Đỏ", "KG-001194", "51H-315.66", "Đặng Thị Yến");
        car(map, "Toyota Corolla Cross 1.8V 2023", 790000000, 23800, "Xăng", "Trắng", "KG-001189", "30L-655.02", "Lý Thu Hằng");
        car(map, "Ford Everest 2.0L 2021", 995000000, 61200, "Dầu", "Xám", "KG-001188", "51H-802.37", "Hồ Văn Long");
        car(map, "Peugeot 3008 1.6 AT 2021", 830000000, 49700, "Xăng", "Xanh dương", "KG-001190", "51K-430.18", "Cao Minh Trí");
        car(map, "Hyundai Elantra 1.6 AT 2022", 615000000, 31700, "Xăng", "Trắng", "KG-001180", "51K-286.59", "Châu Minh Tâm");
        car(map, "Hyundai Santa Fe 2.2D 2021", 985000000, 52300, "Dầu", "Đen", "KG-001185", "30K-518.29", "Đinh Thị Thảo");
        car(map, "Mercedes-Benz C200 2020", 1150000000, 64300, "Xăng", "Đen", "KG-001178", "51H-130.64", "Hà Thị Dung");
        car(map, "Mazda 6 2.0 Premium 2020", 640000000, 68100, "Xăng", "Trắng", "KG-001183", "30G-225.76", "Tạ Thị Nga");
        car(map, "Suzuki XL7 1.5 AT 2022", 545000000, 38900, "Xăng", "Cam", "KG-001186", "51K-172.65", "Tô Văn Hưng");
        car(map, "Kia K3 1.6 Premium 2022", 575000000, 29400, "Xăng", "Đỏ", "KG-001184", "51G-604.11", "Võ Quốc Bảo");
        car(map, "VinFast Lux A2.0 2021", 690000000, 44800, "Xăng", "Đen", "KG-001181", "30K-361.48", "Kiều Thị Oanh");
        car(map, "Nissan Navara 2.5 AT 2021", 630000000, 57600, "Dầu", "Xám", "KG-001179", "30L-493.07", "Thái Văn Lộc");
        return map;
    }

    /** Thông số xe đi kèm mã hồ sơ ký gửi, vì Sales chỉ bán xe đã có hồ sơ. Khi nối BE lấy từ Consignment và Vehicle. */
    private static void car(Map<String, Map<String, Object>> map, String title, int price, int km, String fuel,
            String color, String file, String plate, String owner) {
        map.put(title, Map.of("price", price, "km", km, "fuel", fuel, "color", color, "file", file, "plate", plate,
                "owner", owner, "vin", "RLVN2026" + String.format("%09d", Integer.parseInt(file.substring(3)))));
    }

    private static List<String> slots() {
        List<String> times = new ArrayList<>();
        for (int hour = 8; hour < 18; hour++) {
            times.add(String.format("%02d:00", hour));
            times.add(String.format("%02d:30", hour));
        }
        return times;
    }

    static Map<String, String> pairs(String... values) {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> list = Arrays.asList(values);
        for (int i = 0; i < list.size(); i += 2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }
}
