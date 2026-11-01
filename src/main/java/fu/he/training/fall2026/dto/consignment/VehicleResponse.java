package fu.he.training.fall2026.dto.consignment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VehicleResponse {

    Long id;
    String vin;
    String displayLabel;
    Integer year;
}
