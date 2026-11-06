package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.listing.PublicListingCardResponse;
import fu.he.training.fall2026.dto.listing.PublicListingFilter;
import java.util.List;
import org.springframework.data.domain.Page;

/** Tin bán xe công khai: chỉ tin LISTED. */
public interface PublicSaleNewsService {

    /** Six cards per page; a page past the end returns the last page. */
    Page<PublicListingCardResponse> search(PublicListingFilter filter);

    /** Brands that have at least one listed car. */
    List<String> listBrands();

    /** Newest listed cars for the home carousel. */
    List<PublicListingCardResponse> featured();
}
