package fu.he.training.fall2026.dto.inspection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectionResultRequest {

    @NotNull(message = "Vui lòng chọn loại kiểm định.")
    private Long checklistId;

    /** One row per step of the chosen inspection type, in step order. */
    @Valid
    private List<InspectionStepResultRequest> steps = new ArrayList<>();

    @NotNull(message = "Vui lòng nhập giá chào bán.")
    @DecimalMin(value = "0", inclusive = false, message = "Giá chào bán phải lớn hơn 0.")
    private BigDecimal proposedPrice;

    @Size(max = 4000, message = "Ghi chú định giá tối đa 4000 ký tự.")
    private String rationale;
}
