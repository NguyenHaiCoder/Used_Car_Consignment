package fu.he.training.fall2026.constant.sales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nhãn / danh mục cố định cho màn Tư vấn & lịch xem xe (không chứa data nghiệp vụ). */
public final class SalesInquiryCatalog {

    public static final Map<String, Map<String, String>> STATUSES = statuses();
    public static final Map<String, String> VIEWING_NAMES = pairs(
            "BOOKED", "Đã đặt", "CONFIRMED", "Đã xác nhận", "DONE", "Đã xem", "CANCELLED", "Đã hủy");
    public static final Map<String, String> VIEWING_PILLS = pairs(
            "BOOKED", "pending", "CONFIRMED", "approved", "DONE", "closed", "CANCELLED", "closed-red");
    public static final Map<String, String> CHANNELS = pairs(
            "WEBSITE", "Website", "PHONE", "Điện thoại", "SHOWROOM", "Tại showroom");
    public static final Map<String, String> NEEDS = pairs(
            "PRICE", "Hỏi giá", "VIEWING", "Xem xe", "TEST_DRIVE", "Lái thử");
    public static final Map<String, String> VIEWING_TYPES = pairs(
            "VIEWING", "Xem xe", "TEST_DRIVE", "Lái thử");
    public static final List<String> PLACES = List.of("Showroom Hòa Lạc", "Showroom Cầu Giấy");
    public static final List<String> CLOSE_REASONS = List.of(
            "Không quan tâm", "Đã mua xe khác", "Không liên lạc được", "Ngân sách không phù hợp", "Lý do khác");
    /** Index 0 = Monday … 6 = Sunday (khớp #temporals.dayOfWeek). */
    public static final List<String> DAY_NAMES = List.of(
            "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật");
    public static final List<String> SLOTS = slots();
    public static final List<Integer> DURATIONS = List.of(30, 45, 60, 90);

    private SalesInquiryCatalog() {
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] words = name.trim().split("\\s+");
        String last = words[words.length - 1].substring(0, 1);
        return words.length == 1
                ? last.toUpperCase()
                : (words[words.length - 2].charAt(0) + last).toUpperCase();
    }

    public static Map<String, String> pairs(String... values) {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> list = Arrays.asList(values);
        for (int i = 0; i + 1 < list.size(); i += 2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }

    private static Map<String, Map<String, String>> statuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("NEW", status("Yêu cầu mới", "violet", "bell", "new"));
        map.put("OPEN", status("Đang tư vấn", "blue", "chat", "open"));
        map.put("SCHEDULED", status("Đã đặt lịch xem", "amber", "calendar-check", "scheduled"));
        map.put("VIEWED", status("Đã xem xe", "green", "check", "viewed"));
        map.put("CONVERTED", status("Đã đặt cọc", "green", "coins", "converted"));
        map.put("CLOSED", status("Đóng tư vấn", "red", "close", "closed-red"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private static List<String> slots() {
        List<String> times = new ArrayList<>();
        for (int hour = 8; hour < 18; hour++) {
            times.add(String.format("%02d:00", hour));
            times.add(String.format("%02d:30", hour));
        }
        return times;
    }
}
