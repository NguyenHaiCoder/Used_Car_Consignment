package fu.he.training.fall2026.controller.web.consignment;

import java.util.List;
import java.util.function.IntFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Phân trang UI mock cho danh mục (hãng / dòng / loại xe / loại kiểm định). */
public final class CatalogPageSupport {

    public static final int DEFAULT_SIZE = 10;

    private CatalogPageSupport() {}

    public static int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return switch (size) {
            case 5, 10, 20 -> size;
            default -> DEFAULT_SIZE;
        };
    }

    /** Số trang xin vượt quá dữ liệu (đã xóa bớt, link cũ) thì trả về trang cuối, tránh bảng trống và thanh phân trang đếm ngược. */
    public static <T> Page<T> lastPageIfBeyond(Page<T> result, IntFunction<Page<T>> loadPage) {
        int last = result.getTotalPages() - 1;
        return last >= 0 && result.getNumber() > last ? loadPage.apply(last) : result;
    }

    public static <T> Page<T> pageOf(List<T> all, int page, Integer size) {
        int safeSize = normalizeSize(size);
        int total = all.size();
        int totalPages = total == 0 ? 1 : (int) Math.ceil(total / (double) safeSize);
        int safePage = Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0));
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        return new PageImpl<>(all.subList(from, to), PageRequest.of(safePage, safeSize), total);
    }
}
