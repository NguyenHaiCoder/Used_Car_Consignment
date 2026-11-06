package fu.he.training.fall2026.dto.inspection;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import lombok.Builder;
import lombok.Value;

/** An inspection type as shown on the list, detail and "Kiểm định xe" screens. */
@Value
@Builder
public class InspectionTypeResponse {

    Long id;
    String name;
    Long vehicleTypeId;
    String vehicleTypeCode;
    String vehicleTypeLabel;
    CatalogStatus status;
    long stepCount;
}
