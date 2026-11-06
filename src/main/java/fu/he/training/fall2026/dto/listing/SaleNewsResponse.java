package fu.he.training.fall2026.dto.listing;

import fu.he.training.fall2026.entity.enums.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/** A listing as shown on the "Quản lý tin đăng" list and the edit screen header. */
@Value
@Builder
public class SaleNewsResponse {

    Long id;
    String code;
    String consignmentCode;
    String carLabel;
    String plate;
    Integer odometerKm;
    BigDecimal listPrice;
    LocalDateTime createdAt;
    String staffName;
    ListingStatus status;
}
