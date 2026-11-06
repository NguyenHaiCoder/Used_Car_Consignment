package fu.he.training.fall2026.dto.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Shared pagination metadata for Buyer lists and Inquiry history. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponse {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<Integer> pageItems;

    public static PaginationResponse of(int page, int size, long totalElements, int totalPages) {
        List<Integer> items = new ArrayList<>();
        if (totalPages > 0) {
            items.add(1);
            int start = Math.max(2, page - 2);
            int end = Math.min(totalPages - 1, page + 2);
            if (start > 2) items.add(-1);
            for (int number = start; number <= end; number++) items.add(number);
            if (end < totalPages - 1) items.add(-1);
            if (totalPages > 1) items.add(totalPages);
        }
        return new PaginationResponse(page, size, totalElements, totalPages, items);
    }
}
