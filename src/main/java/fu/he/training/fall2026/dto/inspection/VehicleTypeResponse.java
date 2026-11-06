package fu.he.training.fall2026.dto.inspection;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import lombok.Builder;
import lombok.Value;

/** A vehicle type as shown on the list screen and in the pick-lists. */
@Value
@Builder
public class VehicleTypeResponse {

    Long id;
    String code;
    String label;
    CatalogStatus status;
}
