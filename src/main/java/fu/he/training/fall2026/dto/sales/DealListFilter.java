package fu.he.training.fall2026.dto.sales;

import lombok.Getter;
import lombok.Setter;

/** Bộ lọc danh sách Đặt cọc / Hợp đồng bán xe. */
@Getter
@Setter
public class DealListFilter {

    private String q = "";
    private String status = "";
    private String staff = "";
    private String from = "";
    private String to = "";
    private String amountMin = "";
    private String amountMax = "";
    /** Chuỗi số trang — parse an toàn ở service (tránh 500 khi ?page=abc). */
    private String page = "";
    private String size;
}
