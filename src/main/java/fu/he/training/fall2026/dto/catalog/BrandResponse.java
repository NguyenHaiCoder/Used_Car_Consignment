package fu.he.training.fall2026.dto.catalog;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BrandResponse {

    Long id;
    /** Mã hiển thị, ví dụ HX-1. */
    String code;
    String name;
    CatalogStatus status;
    String note;
    /** Số dòng xe thuộc hãng (đếm từ vehicle_models). */
    long modelCount;
}
