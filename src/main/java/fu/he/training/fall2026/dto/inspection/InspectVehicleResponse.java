package fu.he.training.fall2026.dto.inspection;

import java.math.BigDecimal;
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
    /** Giá mong muốn của chủ xe. Giá chào bán không được thấp hơn mức này. */
    BigDecimal expectedPrice;
}
