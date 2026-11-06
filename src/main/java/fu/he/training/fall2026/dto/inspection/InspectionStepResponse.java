package fu.he.training.fall2026.dto.inspection;

import lombok.Builder;
import lombok.Value;

/** A step as shown on the detail and "Kiểm định xe" screens. */
@Value
@Builder
public class InspectionStepResponse {

    int order;
    String title;
    String hint;
}
