package fu.he.training.fall2026.dto.sales;

import lombok.Getter;
import lombok.Setter;

/** Bộ lọc danh sách Tư vấn & lịch xem xe. */
@Getter
@Setter
public class SalesInquiryFilter {

    private String q = "";
    private String status = "";
    private String viewing = "";
    private String from = "";
    private String to = "";
    /** Nhận String: link do Thymeleaf dựng gửi `selected=`, `page=`, `size=` rỗng, bind thẳng vào số sẽ 500. */
    private String selected;
    private String page;
    private String size;

    public Integer sizeOrNull() {
        return number(size);
    }

    /** Trang hiện tại, rỗng hoặc sai thì về trang đầu. */
    public int pageOrZero() {
        Integer value = number(page);
        return value == null ? 0 : Math.max(value, 0);
    }

    /** Id dòng đang chọn, rỗng hoặc sai thì coi như chưa chọn. */
    public Long selectedOrNull() {
        if (selected == null || selected.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(selected.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer number(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
