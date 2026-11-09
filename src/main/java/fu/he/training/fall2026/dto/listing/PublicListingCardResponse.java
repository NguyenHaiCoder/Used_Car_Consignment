package fu.he.training.fall2026.dto.listing;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/** A listed car card on the public "Tin bán xe" page and the home carousel. */
@Value
@Builder
public class PublicListingCardResponse {

    Long id;
    String code;
    String title;
    BigDecimal listPrice;
    Integer year;
    Integer odometerKm;
    String fuelType;
    /** First photo of the listing, null when it has none. */
    String coverUrl;
    /** LISTED hoặc RESERVED. */
    String status;
}
