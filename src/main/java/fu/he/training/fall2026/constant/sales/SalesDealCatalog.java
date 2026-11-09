package fu.he.training.fall2026.constant.sales;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nhãn / điều khoản cố định cho Đặt cọc & Hợp đồng bán xe. */
public final class SalesDealCatalog {

    public static final Map<String, Map<String, String>> HOLD_STATUSES = holdStatuses();
    public static final Map<String, Map<String, String>> CONTRACT_STATUSES = contractStatuses();
    public static final Map<String, String> DEPOSIT_DECISIONS =
            SalesInquiryCatalog.pairs("REFUND", "Hoàn cọc", "KEEP", "Giữ cọc");

    public static final String HOLD_TERMS = """
            Bên bán giữ xe cho khách đến hết hạn giữ, không bán cho khách khác trong thời gian này.
            Tiền cọc được trừ vào giá bán khi lập hợp đồng.
            Quá hạn mà chưa lập hợp đồng thì phiếu hết hiệu lực, hai bên thống nhất hoàn hay giữ cọc.""";

    public static final String PAYMENT_TERMS = """
            4.1. Tiền cọc được khấu trừ vào giá bán khi thanh toán.
            4.2. Bên B thanh toán đủ số tiền còn lại trước khi nhận bàn giao xe.
            4.3. Bên A chỉ xác nhận đã nhận tiền khi có chứng từ thanh toán hợp lệ.""";

    public static final String HANDOVER_TERMS = """
            5.1. Bên A bàn giao xe, chìa khóa, phụ kiện và giấy tờ xe sau khi Bên B thanh toán đủ.
            5.2. Hai bên lập biên bản bàn giao, ghi rõ số km, số chìa khóa và phụ kiện kèm theo.
            5.3. Bên A phối hợp với Bên B hoàn tất thủ tục sang tên theo quy định.""";

    public static final String TERMINATION_TERMS = """
            6.1. Hợp đồng hoàn tất khi đủ ba mốc: đã thanh toán, đã sang tên, đã bàn giao.
            6.2. Bên B hủy mua sau khi ký thì tiền cọc được xử lý theo phiếu đặt cọc.
            6.3. Bên A không bàn giao được xe thì hoàn lại toàn bộ số tiền đã nhận.""";

    private SalesDealCatalog() {
    }

    private static Map<String, Map<String, String>> holdStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("ACTIVE", status("Đang giữ xe", "amber", "coins", "pending"));
        map.put("CONVERTED", status("Đã lập hợp đồng", "green", "handshake", "listed"));
        map.put("EXPIRED", status("Hết hạn giữ", "slate", "clock", "closed"));
        map.put("CANCELLED", status("Đã hủy", "red", "close", "sold"));
        return map;
    }

    private static Map<String, Map<String, String>> contractStatuses() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("DRAFT", status("Lưu nháp", "slate", "pencil", "draft"));
        map.put("SIGNED", status("Đã ký", "blue", "handshake", "signed"));
        map.put("ACTIVE", status("Đang thực hiện", "amber", "hourglass", "pending"));
        map.put("COMPLETED", status("Hoàn tất", "green", "check", "listed"));
        map.put("CANCELLED", status("Đã hủy", "red", "close", "sold"));
        return map;
    }

    private static Map<String, String> status(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }
}
