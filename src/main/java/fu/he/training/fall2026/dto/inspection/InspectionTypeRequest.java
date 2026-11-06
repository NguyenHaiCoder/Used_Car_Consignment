package fu.he.training.fall2026.dto.inspection;

import fu.he.training.fall2026.entity.enums.CatalogStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectionTypeRequest {

    /** Hidden field: empty when creating, the inspection type id when editing. */
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên loại kiểm định.")
    @Size(max = 160, message = "Tên loại kiểm định tối đa 160 ký tự.")
    private String name;

    @NotNull(message = "Vui lòng chọn loại xe.")
    private Long vehicleTypeId;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private CatalogStatus status = CatalogStatus.ACTIVE;

    /** Steps in screen order; the step number is the position in this list. */
    @Valid
    @NotEmpty(message = "Cần ít nhất một bước kiểm định.")
    private List<InspectionStepRequest> steps = new ArrayList<>();
}
