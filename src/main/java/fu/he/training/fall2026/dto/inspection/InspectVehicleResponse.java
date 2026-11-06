package fu.he.training.fall2026.dto.inspection;

import lombok.Builder;
import lombok.Value;

/** The consignment shown in the "Xe đang kiểm định" box. */
@Value
@Builder
public class InspectVehicleResponse {

    /** The consignment id. */
    Long id;
    String code;
    String displayLabel;
    String vin;
}
