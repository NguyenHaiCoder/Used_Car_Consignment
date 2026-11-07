package fu.he.training.fall2026.dto.catalog;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {

    /** Hidden: null khi tạo, có id khi sửa. */
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên hãng.")
    @Size(max = 120, message = "Tên hãng tối đa 120 ký tự.")
    private String name;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
    private String note;
}
