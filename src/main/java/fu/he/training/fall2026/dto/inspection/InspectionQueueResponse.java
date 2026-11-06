package fu.he.training.fall2026.dto.inspection;

import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** A consignment row on the "Kiểm định xe" queue. */
@Value
@Builder
public class InspectionQueueResponse {

    /** The consignment id. */
    Long id;
    String code;
    String displayLabel;
    Integer year;
    String vin;
    String consignorName;
    Integer odometerKm;
    LocalDate intakeDate;
    /** Date of the latest inspection of this consignment, null when not inspected yet. */
    LocalDate inspectedOn;
    ConsignmentStatus status;
}
