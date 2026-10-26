package fu.he.training.fall2026.dto.consignment;

import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** A consignment as shown on the list and detail screens. */
@Value
@Builder
public class ConsignmentResponse {

    Long id;
    String code;
    Long consignorId;
    String consignorName;
    VehicleResponse vehicle;
    ConsignmentStatus status;
    BigDecimal expectedPrice;
    LocalDate intakeDate;
    String termNotes;
    boolean ownerConsent;
}
