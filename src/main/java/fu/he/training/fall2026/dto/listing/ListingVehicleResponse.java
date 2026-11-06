package fu.he.training.fall2026.dto.listing;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** The inspected consignment shown in the "Xe đăng bán" box (vehicle data as entered on the consignment form). */
@Value
@Builder
public class ListingVehicleResponse {

    Long consignmentId;
    String code;
    String carLabel;
    Integer year;
    String ownerName;
    LocalDate inspectedOn;
    String plate;
    Integer odometerKm;
    String color;
    String fuelType;
    String vin;
    BigDecimal proposedPrice;
}
