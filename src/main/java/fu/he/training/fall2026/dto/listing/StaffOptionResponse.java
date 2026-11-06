package fu.he.training.fall2026.dto.listing;

import lombok.Builder;
import lombok.Value;

/** A staff member in the "Người phụ trách" filter. */
@Value
@Builder
public class StaffOptionResponse {

    Long id;
    String fullName;
}
