package fu.he.training.fall2026.dto.inspection;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleTypeRequest {

    /** Hidden field: empty when creating, the vehicle type id when editing. */
    private Long id;

    @NotBlank(message = "Vui lòng nhập mã loại xe.")
    @Size(max = 40, message = "Mã loại xe tối đa 40 ký tự.")
    private String code;

    @NotBlank(message = "Vui lòng nhập tên hiển thị.")
    @Size(max = 120, message = "Tên hiển thị tối đa 120 ký tự.")
    private String label;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private CatalogStatus status = CatalogStatus.ACTIVE;
}
