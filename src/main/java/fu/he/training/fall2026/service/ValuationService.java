package fu.he.training.fall2026.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface ValuationService {

    /** Latest proposed price of the consignment formatted as "750.000.000", or "" when it has not been valued. */
    String listingPriceText(String consignmentCode);

    /** Giá chào bán mới nhất sau kiểm định, trống nếu hồ sơ chưa định giá. */
    Optional<BigDecimal> latestProposedPrice(String consignmentCode);
}
