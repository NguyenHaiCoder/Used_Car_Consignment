package fu.he.training.fall2026.dto.catalog;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VehicleModelResponse {

    Long id;
    /** Mã hiển thị, ví dụ DX-1. */
    String code;
    Long brandId;
    String brandName;
    String name;
    CatalogStatus status;
    String note;
}
