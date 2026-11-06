package fu.he.training.fall2026.dto.listing;

import fu.he.training.fall2026.entity.enums.ListingStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Filters of the "Quản lý tin đăng" list, bound from the GET form. */
@Getter
@Setter
public class SaleNewsFilter {

    private String q;

    private ListingStatus status;

    private Long staffId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    /** Million VND. */
    private Integer priceMin;

    /** Million VND. */
    private Integer priceMax;
}
