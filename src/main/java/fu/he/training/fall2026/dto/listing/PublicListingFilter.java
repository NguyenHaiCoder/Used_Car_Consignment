package fu.he.training.fall2026.dto.listing;

import lombok.Getter;
import lombok.Setter;

/** Filters of the public "Tin bán xe" page, bound from the GET form; unknown values fall back to no filter. */
@Getter
@Setter
public class PublicListingFilter {

    private String q;

    private String brand;

    /** under-500, 500-800, 800-1000 or over-1000. */
    private String budget;

    /** newest, price-asc, price-desc or km-asc. */
    private String sort = "newest";

    /** 1-based. */
    private int page = 1;
}
